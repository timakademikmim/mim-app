begin;

create table if not exists public.riwayat_kelas_santri (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id) on delete restrict,
  santri_id uuid not null references public.santri(id) on delete cascade,
  kelas_id uuid references public.kelas(id) on delete set null,
  tahun_ajaran_id uuid references public.tahun_ajaran(id) on delete set null,
  status text not null default 'aktif',
  tanggal_mulai date,
  tanggal_selesai date,
  catatan text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.riwayat_kelas_santri
  add column if not exists tenant_id uuid references public.tenants(id) on delete restrict,
  add column if not exists santri_id uuid references public.santri(id) on delete cascade,
  add column if not exists kelas_id uuid references public.kelas(id) on delete set null,
  add column if not exists tahun_ajaran_id uuid references public.tahun_ajaran(id) on delete set null,
  add column if not exists status text not null default 'aktif',
  add column if not exists tanggal_mulai date,
  add column if not exists tanggal_selesai date,
  add column if not exists catatan text,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

create unique index if not exists riwayat_kelas_santri_student_class_year_key
  on public.riwayat_kelas_santri (santri_id, kelas_id, tahun_ajaran_id);

create index if not exists riwayat_kelas_santri_tenant_idx
  on public.riwayat_kelas_santri (tenant_id);

create index if not exists riwayat_kelas_santri_student_year_idx
  on public.riwayat_kelas_santri (santri_id, tahun_ajaran_id);

alter table public.riwayat_kelas_santri enable row level security;
grant select, insert, update, delete on table public.riwayat_kelas_santri to authenticated;
revoke all on table public.riwayat_kelas_santri from anon;

drop policy if exists riwayat_kelas_santri_tenant_allow on public.riwayat_kelas_santri;
create policy riwayat_kelas_santri_tenant_allow
on public.riwayat_kelas_santri
as permissive
for all
to authenticated
using (tenant_id = (select private.current_tenant_id()))
with check (tenant_id = (select private.current_tenant_id()));

drop policy if exists riwayat_kelas_santri_tenant_guard on public.riwayat_kelas_santri;
create policy riwayat_kelas_santri_tenant_guard
on public.riwayat_kelas_santri
as restrictive
for all
to authenticated
using (tenant_id = (select private.current_tenant_id()))
with check (tenant_id = (select private.current_tenant_id()));

drop trigger if exists enforce_tenant_riwayat_kelas_santri on public.riwayat_kelas_santri;
create trigger enforce_tenant_riwayat_kelas_santri
before insert or update on public.riwayat_kelas_santri
for each row execute function private.enforce_authenticated_row_tenant();

drop trigger if exists set_updated_at_riwayat_kelas_santri on public.riwayat_kelas_santri;
create trigger set_updated_at_riwayat_kelas_santri
before update on public.riwayat_kelas_santri
for each row execute function private.set_updated_at();

do $$
begin
  if to_regprocedure('private.current_session_satisfies_mfa()') is not null then
    drop policy if exists riwayat_kelas_santri_mfa_guard on public.riwayat_kelas_santri;
    create policy riwayat_kelas_santri_mfa_guard
    on public.riwayat_kelas_santri
    as restrictive
    for all
    to authenticated
    using ((select private.current_session_satisfies_mfa()))
    with check ((select private.current_session_satisfies_mfa()));
  end if;
end;
$$;

comment on table public.riwayat_kelas_santri is
  'Riwayat kelas santri per tahun ajaran. Naik kelas memindahkan baris santri utama tanpa membuat duplikat.';

commit;
