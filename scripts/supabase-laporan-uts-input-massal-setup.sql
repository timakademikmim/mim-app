create table if not exists public.laporan_uts_input_massal (
  id uuid primary key default gen_random_uuid(),
  guru_id uuid not null,
  semester_id uuid not null,
  kelas_id uuid null,
  santri_id uuid not null,
  override_json jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

create unique index if not exists laporan_uts_input_massal_unique
  on public.laporan_uts_input_massal (guru_id, semester_id, santri_id);

create index if not exists laporan_uts_input_massal_semester_idx
  on public.laporan_uts_input_massal (semester_id);

create index if not exists laporan_uts_input_massal_santri_idx
  on public.laporan_uts_input_massal (santri_id);

create or replace function public.set_laporan_uts_input_massal_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists laporan_uts_input_massal_set_updated_at
  on public.laporan_uts_input_massal;

create trigger laporan_uts_input_massal_set_updated_at
before update on public.laporan_uts_input_massal
for each row
execute function public.set_laporan_uts_input_massal_updated_at();
