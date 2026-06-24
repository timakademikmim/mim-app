create table if not exists public.rapor_deskripsi_mapel (
  id bigserial primary key,
  distribusi_id text not null,
  guru_id text null,
  mapel_id text not null,
  semester_id text null,
  deskripsi_a_pengetahuan text null,
  deskripsi_b_pengetahuan text null,
  deskripsi_c_pengetahuan text null,
  deskripsi_d_pengetahuan text null,
  deskripsi_e_pengetahuan text null,
  deskripsi_a_keterampilan text null,
  deskripsi_b_keterampilan text null,
  deskripsi_c_keterampilan text null,
  deskripsi_d_keterampilan text null,
  deskripsi_e_keterampilan text null,
  updated_at timestamptz not null default now(),
  unique (distribusi_id)
);

alter table public.rapor_deskripsi_mapel
  add column if not exists guru_id text,
  add column if not exists mapel_id text,
  add column if not exists semester_id text,
  add column if not exists deskripsi_a_pengetahuan text,
  add column if not exists deskripsi_b_pengetahuan text,
  add column if not exists deskripsi_c_pengetahuan text,
  add column if not exists deskripsi_d_pengetahuan text,
  add column if not exists deskripsi_e_pengetahuan text,
  add column if not exists deskripsi_a_keterampilan text,
  add column if not exists deskripsi_b_keterampilan text,
  add column if not exists deskripsi_c_keterampilan text,
  add column if not exists deskripsi_d_keterampilan text,
  add column if not exists deskripsi_e_keterampilan text,
  add column if not exists updated_at timestamptz not null default now();

create unique index if not exists rapor_deskripsi_mapel_distribusi_unique
  on public.rapor_deskripsi_mapel (distribusi_id);

create index if not exists rapor_deskripsi_mapel_mapel_semester_idx
  on public.rapor_deskripsi_mapel (mapel_id, semester_id);

create or replace function public.set_rapor_deskripsi_mapel_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_rapor_deskripsi_mapel_updated_at
  on public.rapor_deskripsi_mapel;

create trigger trg_rapor_deskripsi_mapel_updated_at
before update on public.rapor_deskripsi_mapel
for each row
execute function public.set_rapor_deskripsi_mapel_updated_at();

alter table public.rapor_deskripsi_mapel enable row level security;

drop policy if exists rapor_deskripsi_mapel_all_anon on public.rapor_deskripsi_mapel;
create policy rapor_deskripsi_mapel_all_anon
on public.rapor_deskripsi_mapel
for all
to anon
using (true)
with check (true);

drop policy if exists rapor_deskripsi_mapel_all_authenticated on public.rapor_deskripsi_mapel;
create policy rapor_deskripsi_mapel_all_authenticated
on public.rapor_deskripsi_mapel
for all
to authenticated
using (true)
with check (true);
