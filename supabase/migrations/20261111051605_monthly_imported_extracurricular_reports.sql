begin;

create table if not exists public.laporan_bulanan_ekskul_import (
  id uuid primary key default gen_random_uuid(),
  periode text not null,
  santri_id uuid not null references public.santri(id) on delete cascade,
  kegiatan_key text not null check (btrim(kegiatan_key) <> ''),
  kegiatan_ekskul text not null check (btrim(kegiatan_ekskul) <> ''),
  penanggung_jawab text,
  no_hp_pj text,
  kehadiran text,
  catatan_pj text,
  updated_by uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  tenant_id uuid not null default private.current_tenant_id(),
  constraint laporan_bulanan_ekskul_import_periode_santri_kegiatan_key
    unique (periode, santri_id, kegiatan_key)
);

create index if not exists laporan_bulanan_ekskul_import_tenant_periode_santri_idx
  on public.laporan_bulanan_ekskul_import (tenant_id, periode, santri_id);

alter table public.laporan_bulanan_ekskul_import enable row level security;
grant select, insert, update, delete on table public.laporan_bulanan_ekskul_import to authenticated;

drop policy if exists tenant_laporan_bulanan_ekskul_import_allow on public.laporan_bulanan_ekskul_import;
create policy tenant_laporan_bulanan_ekskul_import_allow
on public.laporan_bulanan_ekskul_import as permissive
for all to authenticated
using (tenant_id = (select private.current_tenant_id()))
with check (tenant_id = (select private.current_tenant_id()));

drop policy if exists tenant_laporan_bulanan_ekskul_import_guard on public.laporan_bulanan_ekskul_import;
create policy tenant_laporan_bulanan_ekskul_import_guard
on public.laporan_bulanan_ekskul_import as restrictive
for all to authenticated
using (tenant_id = (select private.current_tenant_id()))
with check (tenant_id = (select private.current_tenant_id()));

drop policy if exists mfa_guard_laporan_bulanan_ekskul_import on public.laporan_bulanan_ekskul_import;
create policy mfa_guard_laporan_bulanan_ekskul_import
on public.laporan_bulanan_ekskul_import as restrictive
for all to authenticated
using ((select private.current_session_satisfies_mfa()))
with check ((select private.current_session_satisfies_mfa()));

drop trigger if exists enforce_tenant_laporan_bulanan_ekskul_import on public.laporan_bulanan_ekskul_import;
create trigger enforce_tenant_laporan_bulanan_ekskul_import
before insert or update on public.laporan_bulanan_ekskul_import
for each row execute function private.enforce_authenticated_row_tenant();

commit;
