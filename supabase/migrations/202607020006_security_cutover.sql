begin;

do $$
begin
  if exists (
    select 1
    from public.karyawan
    where aktif is true
      and auth_user_id is null
  ) then
    raise exception 'Security cutover dibatalkan: masih ada akun aktif tanpa Supabase Auth';
  end if;
end;
$$;

-- Putuskan seluruh jembatan CRUD anonim pada tabel tenant.
do $$
declare
  table_row record;
  policy_row record;
begin
  for table_row in
    select distinct column_row.table_name
    from information_schema.columns column_row
    join information_schema.tables table_info
      on table_info.table_schema = column_row.table_schema
     and table_info.table_name = column_row.table_name
    where column_row.table_schema = 'public'
      and column_row.column_name = 'tenant_id'
      and table_info.table_type = 'BASE TABLE'
  loop
    for policy_row in
      select policyname
      from pg_policies
      where schemaname = 'public'
        and tablename = table_row.table_name
        and 'anon' = any(roles)
    loop
      execute format('drop policy if exists %I on public.%I', policy_row.policyname, table_row.table_name);
    end loop;
    execute format('revoke all on table public.%I from anon', table_row.table_name);
  end loop;
end;
$$;

-- Password hanya boleh berada di Supabase Auth (bcrypt), bukan tabel aplikasi.
alter table public.karyawan drop column if exists password;

create table if not exists public.password_reset_requests (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  employee_id uuid not null references public.karyawan(id) on delete cascade,
  requester_fingerprint text not null,
  status text not null default 'pending' check (status in ('pending', 'resolved', 'dismissed')),
  requested_at timestamptz not null default now(),
  resolved_at timestamptz null,
  resolved_by uuid null references public.karyawan(id) on delete set null
);
alter table public.password_reset_requests enable row level security;
revoke all on table public.password_reset_requests from anon, authenticated;
create unique index if not exists password_reset_requests_one_pending
  on public.password_reset_requests (employee_id)
  where status = 'pending';
create index if not exists password_reset_requests_fingerprint_requested_idx
  on public.password_reset_requests (requester_fingerprint, requested_at desc);

-- Dokumen dan media ujian bersifat private. Foto profil dan stiker tetap publik.
update storage.buckets
set public = false
where id in ('surat-pemberitahuan', 'soal-ujian-media', 'laporan-bulanan', 'laporan-uts');

drop policy if exists legacy_storage_select_bridge on storage.objects;
drop policy if exists legacy_storage_insert_bridge on storage.objects;
drop policy if exists legacy_storage_update_bridge on storage.objects;
drop policy if exists legacy_storage_delete_bridge on storage.objects;
drop policy if exists legacy_storage_anon_guard on storage.objects;
revoke execute on function private.storage_object_is_legacy(text) from anon;

-- Objek lama Putra tetap dapat dibaca oleh pengguna Putra yang sudah login,
-- tetapi tidak lagi dapat diakses publik atau ditulis lewat jalur legacy.
drop policy if exists tenant_storage_legacy_owner_read on storage.objects;
do $$
declare
  putra_tenant_id uuid;
begin
  select tenant.id
  into putra_tenant_id
  from public.tenants tenant
  join public.organizations organization on organization.id = tenant.organization_id
  where lower(organization.code) = 'mim'
    and lower(tenant.code) = 'putra'
  limit 1;

  if putra_tenant_id is not null then
    execute format(
      'create policy tenant_storage_legacy_owner_read on storage.objects for select to authenticated using (bucket_id = any (array[''surat-pemberitahuan'',''soal-ujian-media'',''laporan-bulanan'',''laporan-uts'']::text[]) and private.storage_object_is_legacy(name) and (select private.current_tenant_id()) = %L::uuid)',
      putra_tenant_id::text
    );
  end if;
end;
$$;

-- Pengguna yang sudah mendaftarkan MFA wajib memakai sesi aal2.
create or replace function private.current_session_satisfies_mfa()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select auth.uid() is not null
    and (
      not exists (
        select 1
        from auth.mfa_factors factor
        where factor.user_id = auth.uid()
          and factor.status = 'verified'
      )
      or coalesce(auth.jwt() ->> 'aal', 'aal1') = 'aal2'
    );
$$;

revoke all on function private.current_session_satisfies_mfa() from public;
grant execute on function private.current_session_satisfies_mfa() to authenticated;

do $$
declare
  table_row record;
  policy_name text;
begin
  for table_row in
    select distinct column_row.table_name
    from information_schema.columns column_row
    join information_schema.tables table_info
      on table_info.table_schema = column_row.table_schema
     and table_info.table_name = column_row.table_name
    where column_row.table_schema = 'public'
      and column_row.column_name = 'tenant_id'
      and table_info.table_type = 'BASE TABLE'
      and column_row.table_name <> 'tenant_audit_logs'
  loop
    policy_name := left('mfa_guard_' || table_row.table_name || '_' || substr(md5(table_row.table_name), 1, 8), 63);
    execute format('drop policy if exists %I on public.%I', policy_name, table_row.table_name);
    execute format(
      'create policy %I on public.%I as restrictive for all to authenticated using ((select private.current_session_satisfies_mfa())) with check ((select private.current_session_satisfies_mfa()))',
      policy_name,
      table_row.table_name
    );
  end loop;
end;
$$;

drop policy if exists tenant_storage_mfa_guard on storage.objects;
create policy tenant_storage_mfa_guard
on storage.objects
as restrictive
for all
to authenticated
using ((select private.current_session_satisfies_mfa()))
with check ((select private.current_session_satisfies_mfa()));

-- Deduplikasi notifikasi harus mempertimbangkan tenant.
drop index if exists public.notification_events_user_dedupe_key;
create unique index if not exists notification_events_tenant_user_dedupe_key
  on public.notification_events (tenant_id, user_id, dedupe_key)
  where dedupe_key is not null;

create index if not exists push_tokens_tenant_user_idx
  on public.push_tokens (tenant_id, user_id);
create index if not exists web_push_subscriptions_tenant_user_active_idx
  on public.web_push_subscriptions (tenant_id, user_id, is_active);

comment on function private.current_session_satisfies_mfa() is
  'Requires aal2 whenever the authenticated user has a verified MFA factor.';

commit;
