begin;

create table if not exists public.pengaturan_absensi_lokasi (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id) on delete restrict,
  nama_lokasi text not null default 'Sekolah',
  alamat_lokasi text not null default '',
  latitude double precision not null,
  longitude double precision not null,
  radius_meter integer not null default 100,
  max_accuracy_meter integer not null default 80,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint pengaturan_absensi_lokasi_radius_check check (radius_meter between 20 and 1000),
  constraint pengaturan_absensi_lokasi_accuracy_check check (max_accuracy_meter between 10 and 500)
);

alter table public.pengaturan_absensi_lokasi
  add column if not exists tenant_id uuid references public.tenants(id) on delete restrict,
  add column if not exists nama_lokasi text not null default 'Sekolah',
  add column if not exists alamat_lokasi text not null default '',
  add column if not exists latitude double precision,
  add column if not exists longitude double precision,
  add column if not exists radius_meter integer not null default 100,
  add column if not exists max_accuracy_meter integer not null default 80,
  add column if not exists active boolean not null default true,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

create index if not exists pengaturan_absensi_lokasi_tenant_active_idx
  on public.pengaturan_absensi_lokasi (tenant_id, active, updated_at desc);

create table if not exists public.absensi_karyawan_lokasi (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id) on delete restrict,
  guru_id uuid not null references public.karyawan(id) on delete cascade,
  tanggal date not null,
  datang_at timestamptz,
  datang_latitude double precision,
  datang_longitude double precision,
  datang_accuracy_meter double precision,
  datang_distance_meter double precision,
  datang_valid boolean not null default false,
  datang_mock_detected boolean not null default false,
  pulang_at timestamptz,
  pulang_latitude double precision,
  pulang_longitude double precision,
  pulang_accuracy_meter double precision,
  pulang_distance_meter double precision,
  pulang_valid boolean not null default false,
  pulang_mock_detected boolean not null default false,
  status text not null default 'hadir',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint absensi_karyawan_lokasi_status_check check (status in ('hadir', 'tidak_lengkap'))
);

alter table public.absensi_karyawan_lokasi
  add column if not exists tenant_id uuid references public.tenants(id) on delete restrict,
  add column if not exists guru_id uuid references public.karyawan(id) on delete cascade,
  add column if not exists tanggal date,
  add column if not exists datang_at timestamptz,
  add column if not exists datang_latitude double precision,
  add column if not exists datang_longitude double precision,
  add column if not exists datang_accuracy_meter double precision,
  add column if not exists datang_distance_meter double precision,
  add column if not exists datang_valid boolean not null default false,
  add column if not exists datang_mock_detected boolean not null default false,
  add column if not exists pulang_at timestamptz,
  add column if not exists pulang_latitude double precision,
  add column if not exists pulang_longitude double precision,
  add column if not exists pulang_accuracy_meter double precision,
  add column if not exists pulang_distance_meter double precision,
  add column if not exists pulang_valid boolean not null default false,
  add column if not exists pulang_mock_detected boolean not null default false,
  add column if not exists status text not null default 'hadir',
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

update public.absensi_karyawan_lokasi
set status = case
  when datang_at is not null and pulang_at is null then 'tidak_lengkap'
  else 'hadir'
end
where status is null or status = '';

alter table public.absensi_karyawan_lokasi
  alter column tenant_id set not null,
  alter column guru_id set not null,
  alter column tanggal set not null,
  alter column datang_valid set not null,
  alter column datang_mock_detected set not null,
  alter column pulang_valid set not null,
  alter column pulang_mock_detected set not null,
  alter column status set not null,
  alter column created_at set not null,
  alter column updated_at set not null;

do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conrelid = 'public.absensi_karyawan_lokasi'::regclass
      and conname = 'absensi_karyawan_lokasi_status_check'
  ) then
    alter table public.absensi_karyawan_lokasi
      add constraint absensi_karyawan_lokasi_status_check check (status in ('hadir', 'tidak_lengkap'));
  end if;
end;
$$;

create unique index if not exists absensi_karyawan_lokasi_teacher_date_key
  on public.absensi_karyawan_lokasi (tenant_id, guru_id, tanggal);

create index if not exists absensi_karyawan_lokasi_tenant_date_idx
  on public.absensi_karyawan_lokasi (tenant_id, tanggal desc);

create index if not exists absensi_karyawan_lokasi_guru_date_idx
  on public.absensi_karyawan_lokasi (guru_id, tanggal desc);

alter table public.pengaturan_absensi_lokasi enable row level security;
alter table public.absensi_karyawan_lokasi enable row level security;

grant select, insert, update, delete on table public.pengaturan_absensi_lokasi to authenticated;
grant select, insert, update, delete on table public.absensi_karyawan_lokasi to authenticated;
revoke all on table public.pengaturan_absensi_lokasi from anon;
revoke all on table public.absensi_karyawan_lokasi from anon;

create or replace function private.current_employee_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
  select employee.id
  from public.karyawan employee
  where employee.auth_user_id = (select auth.uid())
    and employee.tenant_id = (select private.current_tenant_id())
    and employee.aktif is true
  limit 1
$$;

revoke all on function private.current_employee_id() from public;
grant execute on function private.current_employee_id() to authenticated;

drop policy if exists pengaturan_absensi_lokasi_tenant_allow on public.pengaturan_absensi_lokasi;
drop policy if exists pengaturan_absensi_lokasi_tenant_guard on public.pengaturan_absensi_lokasi;
drop policy if exists pengaturan_absensi_lokasi_anon_bridge on public.pengaturan_absensi_lokasi;
drop policy if exists pengaturan_absensi_lokasi_select on public.pengaturan_absensi_lokasi;
create policy pengaturan_absensi_lokasi_select
on public.pengaturan_absensi_lokasi
as permissive
for select
to authenticated
using (tenant_id = (select private.current_tenant_id()));

drop policy if exists pengaturan_absensi_lokasi_admin_insert on public.pengaturan_absensi_lokasi;
create policy pengaturan_absensi_lokasi_admin_insert
on public.pengaturan_absensi_lokasi
as permissive
for insert
to authenticated
with check (
  tenant_id = (select private.current_tenant_id())
  and (select private.current_user_has_role('admin'))
);

drop policy if exists pengaturan_absensi_lokasi_admin_update on public.pengaturan_absensi_lokasi;
create policy pengaturan_absensi_lokasi_admin_update
on public.pengaturan_absensi_lokasi
as permissive
for update
to authenticated
using (
  tenant_id = (select private.current_tenant_id())
  and (select private.current_user_has_role('admin'))
)
with check (
  tenant_id = (select private.current_tenant_id())
  and (select private.current_user_has_role('admin'))
);

drop policy if exists pengaturan_absensi_lokasi_admin_delete on public.pengaturan_absensi_lokasi;
create policy pengaturan_absensi_lokasi_admin_delete
on public.pengaturan_absensi_lokasi
as permissive
for delete
to authenticated
using (
  tenant_id = (select private.current_tenant_id())
  and (select private.current_user_has_role('admin'))
);

drop policy if exists absensi_karyawan_lokasi_tenant_allow on public.absensi_karyawan_lokasi;
drop policy if exists absensi_karyawan_lokasi_tenant_guard on public.absensi_karyawan_lokasi;
drop policy if exists absensi_karyawan_lokasi_anon_bridge on public.absensi_karyawan_lokasi;
drop policy if exists absensi_karyawan_lokasi_select_self on public.absensi_karyawan_lokasi;
create policy absensi_karyawan_lokasi_select_self
on public.absensi_karyawan_lokasi
as permissive
for select
to authenticated
using (
  tenant_id = (select private.current_tenant_id())
  and (
    guru_id = (select private.current_employee_id())
    or (select private.current_user_has_role('admin'))
    or (select private.current_user_has_role('wakasek akademik'))
    or (select private.current_user_has_role('wakasek kurikulum'))
    or (select private.current_user_has_role('wakasek bidang akademik'))
    or (select private.current_user_has_role('wakasek bidang kurikulum'))
  )
);

drop policy if exists absensi_karyawan_lokasi_insert_self on public.absensi_karyawan_lokasi;
create policy absensi_karyawan_lokasi_insert_self
on public.absensi_karyawan_lokasi
as permissive
for insert
to authenticated
with check (
  tenant_id = (select private.current_tenant_id())
  and guru_id = (select private.current_employee_id())
);

drop policy if exists absensi_karyawan_lokasi_update_self on public.absensi_karyawan_lokasi;
create policy absensi_karyawan_lokasi_update_self
on public.absensi_karyawan_lokasi
as permissive
for update
to authenticated
using (
  tenant_id = (select private.current_tenant_id())
  and guru_id = (select private.current_employee_id())
)
with check (
  tenant_id = (select private.current_tenant_id())
  and guru_id = (select private.current_employee_id())
);

drop policy if exists absensi_karyawan_lokasi_delete_self_or_admin on public.absensi_karyawan_lokasi;
create policy absensi_karyawan_lokasi_delete_self_or_admin
on public.absensi_karyawan_lokasi
as permissive
for delete
to authenticated
using (
  tenant_id = (select private.current_tenant_id())
  and (
    guru_id = (select private.current_employee_id())
    or (select private.current_user_has_role('admin'))
  )
);

drop trigger if exists enforce_tenant_pengaturan_absensi_lokasi on public.pengaturan_absensi_lokasi;
create trigger enforce_tenant_pengaturan_absensi_lokasi
before insert or update on public.pengaturan_absensi_lokasi
for each row execute function private.enforce_authenticated_row_tenant();

drop trigger if exists set_updated_at_pengaturan_absensi_lokasi on public.pengaturan_absensi_lokasi;
create trigger set_updated_at_pengaturan_absensi_lokasi
before update on public.pengaturan_absensi_lokasi
for each row execute function private.set_updated_at();

drop trigger if exists enforce_tenant_absensi_karyawan_lokasi on public.absensi_karyawan_lokasi;
create trigger enforce_tenant_absensi_karyawan_lokasi
before insert or update on public.absensi_karyawan_lokasi
for each row execute function private.enforce_authenticated_row_tenant();

drop trigger if exists set_updated_at_absensi_karyawan_lokasi on public.absensi_karyawan_lokasi;
create trigger set_updated_at_absensi_karyawan_lokasi
before update on public.absensi_karyawan_lokasi
for each row execute function private.set_updated_at();

insert into public.pengaturan_absensi_lokasi (
  tenant_id,
  nama_lokasi,
  alamat_lokasi,
  latitude,
  longitude,
  radius_meter,
  max_accuracy_meter,
  active
)
select
  tenant.id,
  'Pesantren Markaz Imam Malik',
  'Jalan Kajenjeng Raya, Kp. Kanjenjeng, Kassi, Tamangapa, Kec. Manggala, Kota Makassar, Sulawesi Selatan 90235',
  -5.184700,
  119.497528,
  100,
  80,
  true
from public.tenants tenant
where not exists (
  select 1
  from public.pengaturan_absensi_lokasi config
  where config.tenant_id = tenant.id
    and config.active is true
);

comment on table public.pengaturan_absensi_lokasi is
  'Titik lokasi dan radius absensi kedatangan/pulang karyawan per tenant/unit.';

comment on table public.absensi_karyawan_lokasi is
  'Absensi kedatangan dan kepulangan guru/karyawan berbasis validasi lokasi perangkat.';

commit;
