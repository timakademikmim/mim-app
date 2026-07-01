begin;

create extension if not exists pgcrypto;

create schema if not exists private;
revoke all on schema private from public;

create table if not exists public.organizations (
  id uuid primary key default gen_random_uuid(),
  code text not null,
  name text not null,
  legal_name text null,
  address text null,
  phone text null,
  email text null,
  website text null,
  logo_url text null,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint organizations_code_not_blank check (btrim(code) <> ''),
  constraint organizations_name_not_blank check (btrim(name) <> '')
);

create unique index if not exists organizations_code_unique_idx
  on public.organizations (lower(code));

create table if not exists public.tenants (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references public.organizations(id) on delete restrict,
  code text not null,
  slug text not null,
  name text not null,
  official_name text null,
  unit_type text not null default 'school',
  address text null,
  phone text null,
  email text null,
  website text null,
  logo_url text null,
  active boolean not null default true,
  created_by uuid null references auth.users(id) on delete set null,
  updated_by uuid null references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint tenants_code_not_blank check (btrim(code) <> ''),
  constraint tenants_slug_not_blank check (btrim(slug) <> ''),
  constraint tenants_name_not_blank check (btrim(name) <> '')
);

create unique index if not exists tenants_organization_code_unique_idx
  on public.tenants (organization_id, lower(code));

create unique index if not exists tenants_slug_unique_idx
  on public.tenants (lower(slug));

create unique index if not exists tenants_id_organization_unique_idx
  on public.tenants (id, organization_id);

-- Platform administrators bootstrap units and their first unit administrator.
-- Unit users themselves remain one-account-to-one-tenant through karyawan.tenant_id.
create table if not exists public.platform_admins (
  auth_user_id uuid primary key references auth.users(id) on delete cascade,
  organization_id uuid not null references public.organizations(id) on delete cascade,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create or replace function private.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists organizations_set_updated_at on public.organizations;
create trigger organizations_set_updated_at
before update on public.organizations
for each row execute function private.set_updated_at();

drop trigger if exists tenants_set_updated_at on public.tenants;
create trigger tenants_set_updated_at
before update on public.tenants
for each row execute function private.set_updated_at();

drop trigger if exists platform_admins_set_updated_at on public.platform_admins;
create trigger platform_admins_set_updated_at
before update on public.platform_admins
for each row execute function private.set_updated_at();

insert into public.organizations (code, name, legal_name)
values ('mim', 'MIM', 'MIM')
on conflict do nothing;

insert into public.tenants (
  organization_id,
  code,
  slug,
  name,
  official_name,
  unit_type
)
select
  organization.id,
  'putra',
  'putra',
  'MIM Putra',
  'MIM Putra',
  'putra'
from public.organizations organization
where lower(organization.code) = 'mim'
  and not exists (
    select 1
    from public.tenants tenant
    where tenant.organization_id = organization.id
      and lower(tenant.code) = 'putra'
  );

-- Reuse the current school identity for the initial Putra tenant when available.
do $$
declare
  v_name text;
  v_address text;
begin
  if to_regclass('public.struktur_sekolah') is null then
    return;
  end if;

  execute $query$
    select
      nullif(btrim(nama_sekolah), ''),
      nullif(btrim(alamat_sekolah), '')
    from public.struktur_sekolah
    order by updated_at desc nulls last, created_at desc nulls last
    limit 1
  $query$
  into v_name, v_address;

  update public.tenants tenant
  set name = coalesce(v_name, tenant.name),
      official_name = coalesce(v_name, tenant.official_name),
      address = coalesce(v_address, tenant.address),
      updated_at = now()
  from public.organizations organization
  where tenant.organization_id = organization.id
    and lower(organization.code) = 'mim'
    and lower(tenant.code) = 'putra';
end;
$$;

-- Karyawan is the unit account/profile table. A user belongs to exactly one tenant.
alter table if exists public.karyawan
  add column if not exists organization_id uuid,
  add column if not exists tenant_id uuid,
  add column if not exists auth_user_id uuid,
  add column if not exists must_change_password boolean not null default true,
  add column if not exists password_changed_at timestamptz null,
  add column if not exists last_login_at timestamptz null;

do $$
declare
  v_organization_id uuid;
  v_tenant_id uuid;
begin
  select organization.id
  into v_organization_id
  from public.organizations organization
  where lower(organization.code) = 'mim'
  limit 1;

  select tenant.id
  into v_tenant_id
  from public.tenants tenant
  where tenant.organization_id = v_organization_id
    and lower(tenant.code) = 'putra'
  limit 1;

  if v_organization_id is null or v_tenant_id is null then
    raise exception 'Bootstrap organization or Putra tenant was not created';
  end if;

  if to_regclass('public.karyawan') is not null then
    update public.karyawan
    set organization_id = coalesce(organization_id, v_organization_id),
        tenant_id = coalesce(tenant_id, v_tenant_id)
    where organization_id is null
       or tenant_id is null;

    execute format(
      'alter table public.karyawan alter column organization_id set default %L::uuid',
      v_organization_id
    );
    execute format(
      'alter table public.karyawan alter column tenant_id set default %L::uuid',
      v_tenant_id
    );

    alter table public.karyawan alter column organization_id set not null;
    alter table public.karyawan alter column tenant_id set not null;
  end if;
end;
$$;

do $$
begin
  if to_regclass('public.karyawan') is null then
    return;
  end if;

  if not exists (
    select 1
    from pg_constraint
    where conrelid = 'public.karyawan'::regclass
      and conname = 'karyawan_tenant_organization_fkey'
  ) then
    alter table public.karyawan
      add constraint karyawan_tenant_organization_fkey
      foreign key (tenant_id, organization_id)
      references public.tenants(id, organization_id)
      on delete restrict
      not valid;
  end if;

  if not exists (
    select 1
    from pg_constraint
    where conrelid = 'public.karyawan'::regclass
      and conname = 'karyawan_auth_user_fkey'
  ) then
    alter table public.karyawan
      add constraint karyawan_auth_user_fkey
      foreign key (auth_user_id)
      references auth.users(id)
      on delete set null
      not valid;
  end if;

  alter table public.karyawan validate constraint karyawan_tenant_organization_fkey;
  alter table public.karyawan validate constraint karyawan_auth_user_fkey;
end;
$$;

create index if not exists karyawan_tenant_idx
  on public.karyawan (tenant_id);

create index if not exists karyawan_organization_idx
  on public.karyawan (organization_id);

create unique index if not exists karyawan_auth_user_unique_idx
  on public.karyawan (auth_user_id)
  where auth_user_id is not null;

create index if not exists karyawan_tenant_login_idx
  on public.karyawan (tenant_id, lower(id_karyawan));

-- Transitional compatibility list. Every current row belongs to Putra. The
-- default keeps old Android/web clients working until authenticated clients
-- send tenant_id themselves. A later security migration must drop the default.
do $$
declare
  v_tenant_id uuid;
  v_table text;
  v_constraint_name text;
  v_tables text[] := array[
    'santri',
    'kelas',
    'tahun_ajaran',
    'semester',
    'mapel',
    'distribusi_mapel',
    'jadwal_pelajaran',
    'jam_pelajaran',
    'absensi_santri',
    'absensi_pengganti_tugas',
    'absensi_pengajuan_pengganti',
    'nilai_input_akademik',
    'nilai_akademik',
    'rapor_deskripsi_mapel',
    'mapel_patron_materi',
    'mapel_soal_guru',
    'laporan_bulanan_wali',
    'laporan_uts_input_massal',
    'izin_karyawan',
    'izin_santri',
    'ekstrakurikuler',
    'ekstrakurikuler_anggota',
    'ekstrakurikuler_indikator',
    'ekstrakurikuler_progres',
    'ekstrakurikuler_laporan_bulanan',
    'santri_prestasi',
    'santri_pelanggaran',
    'jadwal_ujian',
    'soal_ujian',
    'peserta_ujian',
    'struktur_sekolah',
    'halaqah',
    'halaqah_santri',
    'kamar',
    'kamar_santri',
    'jadwal_halaqah',
    'chat_threads',
    'chat_thread_members',
    'chat_messages',
    'push_tokens',
    'web_push_subscriptions',
    'notification_events',
    'kalender_akademik',
    'tugas_harian_template',
    'tugas_harian_submit',
    'riwayat_kelas_santri'
  ];
begin
  select tenant.id
  into v_tenant_id
  from public.tenants tenant
  join public.organizations organization on organization.id = tenant.organization_id
  where lower(organization.code) = 'mim'
    and lower(tenant.code) = 'putra'
  limit 1;

  if v_tenant_id is null then
    raise exception 'Putra tenant was not found';
  end if;

  foreach v_table in array v_tables loop
    if to_regclass(format('public.%I', v_table)) is null then
      continue;
    end if;

    execute format(
      'alter table public.%I add column if not exists tenant_id uuid',
      v_table
    );
    execute format(
      'update public.%I set tenant_id = $1 where tenant_id is null',
      v_table
    ) using v_tenant_id;
    execute format(
      'alter table public.%I alter column tenant_id set default %L::uuid',
      v_table,
      v_tenant_id
    );
    execute format(
      'alter table public.%I alter column tenant_id set not null',
      v_table
    );
    execute format(
      'create index if not exists %I on public.%I (tenant_id)',
      left('idx_' || v_table || '_tenant', 63),
      v_table
    );

    v_constraint_name := left(
      'fk_' || v_table || '_tenant_' || substr(md5(v_table), 1, 8),
      63
    );

    if not exists (
      select 1
      from pg_constraint
      where conrelid = format('public.%I', v_table)::regclass
        and conname = v_constraint_name
    ) then
      execute format(
        'alter table public.%I add constraint %I foreign key (tenant_id) references public.tenants(id) on delete restrict not valid',
        v_table,
        v_constraint_name
      );
    end if;

    execute format(
      'alter table public.%I validate constraint %I',
      v_table,
      v_constraint_name
    );
  end loop;
end;
$$;

create or replace function private.current_organization_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce(
    (
      select employee.organization_id
      from public.karyawan employee
      where employee.auth_user_id = (select auth.uid())
        and employee.aktif is true
      limit 1
    ),
    (
      select administrator.organization_id
      from public.platform_admins administrator
      where administrator.auth_user_id = (select auth.uid())
        and administrator.active is true
      limit 1
    )
  )
$$;

create or replace function private.current_tenant_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
  select employee.tenant_id
  from public.karyawan employee
  where employee.auth_user_id = (select auth.uid())
    and employee.aktif is true
  limit 1
$$;

create or replace function private.current_user_has_role(requested_role text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.karyawan employee
    where employee.auth_user_id = (select auth.uid())
      and employee.aktif is true
      and lower(btrim(requested_role)) = any (
        regexp_split_to_array(
          lower(coalesce(employee.role, '')),
          '\s*[,|;]\s*'
        )
      )
  )
$$;

create or replace function private.current_user_is_platform_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.platform_admins administrator
    where administrator.auth_user_id = (select auth.uid())
      and administrator.active is true
  )
$$;

create or replace function public.list_login_tenants()
returns table (
  id uuid,
  code text,
  slug text,
  name text,
  official_name text,
  logo_url text
)
language sql
stable
security definer
set search_path = ''
as $$
  select
    tenant.id,
    tenant.code,
    tenant.slug,
    tenant.name,
    tenant.official_name,
    tenant.logo_url
  from public.tenants tenant
  join public.organizations organization
    on organization.id = tenant.organization_id
  where tenant.active is true
    and organization.active is true
  order by tenant.name
$$;

revoke all on function private.current_organization_id() from public;
revoke all on function private.current_tenant_id() from public;
revoke all on function private.current_user_has_role(text) from public;
revoke all on function private.current_user_is_platform_admin() from public;
revoke all on function private.set_updated_at() from public;
revoke all on function public.list_login_tenants() from public;

grant usage on schema private to authenticated;
grant execute on function private.current_organization_id() to authenticated;
grant execute on function private.current_tenant_id() to authenticated;
grant execute on function private.current_user_has_role(text) to authenticated;
grant execute on function private.current_user_is_platform_admin() to authenticated;
grant execute on function public.list_login_tenants() to anon, authenticated;

alter table public.organizations enable row level security;
alter table public.tenants enable row level security;
alter table public.platform_admins enable row level security;

revoke all on table public.organizations from anon;
revoke all on table public.tenants from anon;
revoke all on table public.platform_admins from anon;

revoke all on table public.organizations from authenticated;
revoke all on table public.tenants from authenticated;
revoke all on table public.platform_admins from authenticated;

grant select on table public.organizations to authenticated;
grant select on table public.tenants to authenticated;
grant select on table public.platform_admins to authenticated;

drop policy if exists organizations_select_own on public.organizations;
create policy organizations_select_own
on public.organizations
for select
to authenticated
using (id = (select private.current_organization_id()));

drop policy if exists tenants_select_own on public.tenants;
create policy tenants_select_own
on public.tenants
for select
to authenticated
using (
  id = (select private.current_tenant_id())
  or (
    (select private.current_user_is_platform_admin())
    and organization_id = (select private.current_organization_id())
  )
);

drop policy if exists platform_admins_select_self on public.platform_admins;
create policy platform_admins_select_self
on public.platform_admins
for select
to authenticated
using (auth_user_id = (select auth.uid()));

comment on table public.organizations is
  'Top-level institution that owns one or more school units.';
comment on table public.tenants is
  'School/unit boundary such as Putra, Putri, or SD.';
comment on table public.platform_admins is
  'Trusted organization-level accounts that bootstrap tenants and their first admins.';
comment on column public.karyawan.auth_user_id is
  'Supabase Auth user id. Null only while legacy accounts are being migrated.';
comment on column public.karyawan.tenant_id is
  'The only school/unit this account belongs to.';
comment on column public.karyawan.must_change_password is
  'Requires the account to replace its temporary or migrated password after authentication rollout.';

commit;
