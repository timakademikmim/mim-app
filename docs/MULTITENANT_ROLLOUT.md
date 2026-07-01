# Multitenant Rollout

## Agreed model

- One Supabase project for the MIM organization.
- Units are stored in `tenants`, initially Putra, followed by Putri and SD.
- A unit account belongs to exactly one tenant through `karyawan.tenant_id`.
- There is no many-to-many tenant membership model.
- Roles may still be combined inside one tenant, for example `admin,guru`.
- Platform administrators only bootstrap tenants and their first unit admin.
- Unit admins manage their own unit profile, employees, students, and academic data.
- AI token work is outside this rollout.

## Unit profile ownership

`tenants` is the source of truth for identity used by reports and exam headers:

- unit name and official name
- address
- phone and email
- website
- logo
- active status

`struktur_sekolah` remains tenant-scoped for leadership assignments such as
headmaster, wakasek, wali kelas, musyrif, and muhaffiz. During application
migration, report and exam exporters should read the current tenant profile
instead of a global or hardcoded school profile.

The login screen may call `list_login_tenants()` before authentication. This
RPC intentionally exposes only active unit identity fields. Direct anonymous
access to the `organizations`, `tenants`, and `platform_admins` tables remains
revoked.

## Safe rollout order

1. Take a database backup and schema dump.
2. Apply `202606300001_multitenant_foundation.sql`.
3. Apply `202606300002_multitenant_legacy_tables.sql` for legacy academic
   tables found in the production catalog.
4. Apply `202606300003_tenant_auth_support.sql` and
   `202606300004_authenticated_tenant_rls.sql`, followed by
   `202607010005_tenant_storage_isolation.sql`.
5. Run `scripts/verify-multitenant-foundation.sql` and verify every legacy row
   received the Putra tenant id.
6. Create Supabase Auth users and connect them to `karyawan.auth_user_id`.
7. Update web and Android to send the authenticated user JWT.
8. Replace temporary tenant-wide policies with resource and role-specific
   authorization where required, then secure Storage.
9. Release a mandatory Android update and deploy the updated web app.
10. Remove anonymous bridge policies, remove the temporary Putra defaults, and remove
   the plaintext `karyawan.password` column.
11. Create Putri and SD only after cross-tenant isolation tests pass.

## Authentication identity

Users keep entering a unit, employee ID, and password. Internally, web and
Android derive a stable synthetic Auth email from the selected tenant UUID and
normalized employee ID:

`sha256("<tenant-uuid>:<normalized-employee-id>")@accounts.mim.invalid`

The address is only an internal Supabase Auth identifier. It does not need to
receive email and it does not expose whether an employee account exists. Unit
renames do not change it because the tenant UUID is immutable.

The platform-admin server flow creates a tenant and its first admin. After
that, the unit admin creates, disables, and resets accounts only inside its own
tenant through a JWT-protected Edge Function. The service-role key stays in
Supabase secrets and is never returned to Android or web.

Existing plaintext passwords must not be copied into Supabase Auth. Every
migrated account receives a temporary password and starts with
`must_change_password = true`. Admins can reset passwords but cannot view old
or current passwords.

## Tenant administration Edge Functions

- `manage-tenant` lets platform administrators list, create, edit, and
  deactivate units inside their organization. Units are not physically deleted
  because historical academic data must remain linked.
- `manage-tenant-profile` reads and updates the caller's unit identity. Unit
  admins cannot select another tenant; platform admins are restricted to their
  organization.
- `manage-tenant-user` lists, creates, edits, activates, and resets accounts
  inside the resolved tenant. It never returns or stores the submitted Auth
  password in `karyawan.password`.
- Both functions require a valid Supabase Auth user JWT and reject requests
  made with only the public anon key.
- Administrative changes are written to `tenant_audit_logs` by the server.

Legacy notification functions still accept unauthenticated client calls until
the Auth-capable web and Android clients are deployed. Secure those functions
in the same cutover that removes anonymous table policies so existing clients
do not lose notifications prematurely.

## Android transition client

The Android login screen now loads active units from `list_login_tenants()` and
requires the user to select a unit before entering the employee ID and
password. Auth-linked employees sign in through Supabase Auth. The resulting
access and refresh tokens are encrypted with an Android Keystore AES-GCM key
before being stored in DataStore.

The temporary legacy login path is restricted to rows whose `auth_user_id` is
still null and is always filtered by the selected `tenant_id`. Once an account
is linked to Supabase Auth, it cannot fall back to its old database password.
Remove this compatibility path after every supported employee account has been
migrated.

All non-AI Android repositories now select the bearer token from the active
session. Auth sessions refresh automatically before expiry; legacy accounts
continue to use the anon bridge until they are migrated. Tenant and employee
administration, unit identity, and authenticated self-profile/password changes
use JWT-protected Edge Functions.

Dashboard caches are separated by a hash of tenant UUID and employee ID. This
prevents one unit or account from briefly seeing another account's cached
dashboard on a shared device. Other local draft stores must receive the same
tenant-and-user scoping before Putri or SD production data is added.

## Web transition client

The web login also loads `list_login_tenants()`, derives the same synthetic
Auth email as Android, and prefers Supabase Auth before trying the temporary
legacy login. The fallback query is tenant-scoped and only accepts employees
whose `auth_user_id` is still null.

All role pages share one Supabase client with persisted sessions and automatic
token refresh. Auth-marked sessions fail closed when a REST, Storage, or Edge
Function request would otherwise use the anon bearer. Role pages revalidate
the Auth user, active tenant, active employee, and server-side roles before
continuing. Legacy sessions retain anon behavior only during rollout.

Desktop offline HTTP cache keys include tenant UUID and employee ID. When a
different tenant or account signs in, non-Supabase local storage is cleared so
drafts and page caches from the previous account are not reused. Employee
administration and self-profile/password changes use the protected Edge
Functions for Auth sessions.

## Storage transition

New authenticated uploads in `karyawan-foto`, `surat-pemberitahuan`,
`soal-ujian-media`, `laporan-bulanan`, `laporan-uts`, and `chat-stickers` use
the tenant UUID as the first object path segment. Web and Android derive this
segment from their validated login session, while Storage RLS resolves the
allowed tenant independently from `auth.uid()`.

Migration `202607010005_tenant_storage_isolation.sql` removes older policies
that explicitly reference the managed buckets and replaces them with tenant
policies for authenticated users. A temporary anonymous bridge can only access
legacy paths whose first segment is not a tenant UUID. It cannot write into a
new tenant namespace.

The migration was applied to production on 2026-07-01. A direct Storage API
check returned `400` when the anon key attempted to upload below the Putra
tenant UUID. A temporary legacy-path upload returned `200` and its cleanup also
returned `200`, confirming that the rollout bridge remains compatible without
opening the tenant namespace. Remote database lint reported no issues in the
`private` or `public` schemas; only the pre-existing `extensions.index_advisor`
warnings remain.

The buckets remain public during this transition because reports and letters
currently store and share public URLs. Storage mutation and listing are tenant
isolated, but anyone who obtains a public object URL can still read that file.
Moving sensitive buckets to private access requires a separate URL migration
and signed-download flow.

## Legacy account migration

Production audit on 2026-07-01 initially found 38 active Putra employee
accounts: one Auth account and 37 legacy accounts. All 37 legacy accounts were
migrated that day with distinct random temporary passwords. The final audit
showed 38 linked Auth accounts, zero legacy accounts, 38 unusable
`__AUTH_ONLY__` database password placeholders, and `must_change_password =
true` on every account. A real password-grant login test succeeded and its test
session was immediately logged out.

The temporary credential list is kept only in the local Git-ignored `.private`
directory. The one-time migration Edge Function and its secret were deleted
from production after verification. The admin employee page continues to show
`Auth` or `Legacy`; any future legacy exception can be migrated individually
through the protected `manage-tenant-user` function.

Do not distribute temporary credentials until Auth-capable web and Android
clients are deployed. After mandatory clients are released and verified,
remove the anonymous table and Storage bridges, then remove the plaintext
password column.

## Compatibility warning

The first migration intentionally gives tenant-owned tables a temporary Putra
default. This prevents existing clients from failing when they insert rows
without `tenant_id`. It is only a rollout bridge and must be removed when all
supported clients use Supabase Auth and tenant-aware repositories.

Do not add Putri or SD production data while anonymous table access and the
temporary Putra defaults are still active.

Migration `202606300003_tenant_auth_support.sql` replaces the global employee
ID uniqueness rule with uniqueness on `(tenant_id, id_karyawan)`. Keep this
constraint in place so different units may use the same employee ID without
cross-tenant ambiguity.

Migration `202606300004_authenticated_tenant_rls.sql` enables RLS on current
tenant-owned tables, adds both permissive tenant policies and restrictive
tenant guards for authenticated users, and forces authenticated inserts and
updates to the tenant resolved from `auth.uid()`. Its anon policies are an
explicit temporary compatibility bridge, not the final security posture.
