create extension if not exists "pgcrypto";

create table if not exists public.mapel_soal_guru (
  id uuid primary key default gen_random_uuid(),
  distribusi_id text not null,
  kelas_id text not null,
  mapel_id text not null,
  semester_id text null,
  tahun_ajaran_id text null,
  created_by_guru_id text not null,
  created_by_guru_nama text null,
  updated_by_guru_id text null,
  updated_by_guru_nama text null,
  judul text not null,
  kategori text not null default 'Tugas',
  tanggal date null,
  keterangan text null,
  bentuk_soal text null,
  jumlah_nomor integer null,
  instruksi text null,
  questions_json text null,
  status text not null default 'draft',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists mapel_soal_guru_distribusi_idx
  on public.mapel_soal_guru (distribusi_id, updated_at desc);

create index if not exists mapel_soal_guru_mapel_idx
  on public.mapel_soal_guru (mapel_id, semester_id, tahun_ajaran_id);

create index if not exists mapel_soal_guru_created_by_idx
  on public.mapel_soal_guru (created_by_guru_id);

create or replace function public.set_mapel_soal_guru_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_mapel_soal_guru_updated_at
  on public.mapel_soal_guru;

create trigger trg_mapel_soal_guru_updated_at
before update on public.mapel_soal_guru
for each row
execute function public.set_mapel_soal_guru_updated_at();

alter table public.mapel_soal_guru enable row level security;

drop policy if exists mapel_soal_guru_all_anon on public.mapel_soal_guru;
create policy mapel_soal_guru_all_anon
on public.mapel_soal_guru
for all
to anon
using (true)
with check (true);

drop policy if exists mapel_soal_guru_all_authenticated on public.mapel_soal_guru;
create policy mapel_soal_guru_all_authenticated
on public.mapel_soal_guru
for all
to authenticated
using (true)
with check (true);
