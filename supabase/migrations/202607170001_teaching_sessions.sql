begin;

create table if not exists public.sesi_mengajar (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id) on delete restrict,
  tanggal date not null,
  distribusi_id uuid not null references public.distribusi_mapel(id) on delete cascade,
  lesson_slot_id text not null default '',
  kelas_id uuid references public.kelas(id) on delete set null,
  mapel_id uuid references public.mapel(id) on delete set null,
  semester_id uuid references public.semester(id) on delete set null,
  guru_id uuid references public.karyawan(id) on delete set null,
  materi text not null default '',
  isi_materi text not null default '',
  tugas_judul text not null default '',
  tugas_deskripsi text not null default '',
  assessment_summary_json text not null default '[]',
  status text not null default 'draft',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint sesi_mengajar_status_check check (status in ('draft', 'saved'))
);

alter table public.sesi_mengajar
  add column if not exists tenant_id uuid references public.tenants(id) on delete restrict,
  add column if not exists tanggal date,
  add column if not exists distribusi_id uuid references public.distribusi_mapel(id) on delete cascade,
  add column if not exists lesson_slot_id text not null default '',
  add column if not exists kelas_id uuid references public.kelas(id) on delete set null,
  add column if not exists mapel_id uuid references public.mapel(id) on delete set null,
  add column if not exists semester_id uuid references public.semester(id) on delete set null,
  add column if not exists guru_id uuid references public.karyawan(id) on delete set null,
  add column if not exists materi text not null default '',
  add column if not exists isi_materi text not null default '',
  add column if not exists tugas_judul text not null default '',
  add column if not exists tugas_deskripsi text not null default '',
  add column if not exists assessment_summary_json text not null default '[]',
  add column if not exists status text not null default 'draft',
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

update public.sesi_mengajar
set lesson_slot_id = coalesce(lesson_slot_id, ''),
    materi = coalesce(materi, ''),
    isi_materi = coalesce(isi_materi, ''),
    tugas_judul = coalesce(tugas_judul, ''),
    tugas_deskripsi = coalesce(tugas_deskripsi, ''),
    assessment_summary_json = coalesce(assessment_summary_json, '[]'),
    status = case when status = 'saved' then 'saved' else 'draft' end;

alter table public.sesi_mengajar
  alter column tenant_id set not null,
  alter column tanggal set not null,
  alter column distribusi_id set not null,
  alter column lesson_slot_id set not null,
  alter column materi set not null,
  alter column isi_materi set not null,
  alter column tugas_judul set not null,
  alter column tugas_deskripsi set not null,
  alter column assessment_summary_json set not null,
  alter column status set not null,
  alter column created_at set not null,
  alter column updated_at set not null;

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conrelid = 'public.sesi_mengajar'::regclass
      and conname = 'sesi_mengajar_status_check'
  ) then
    alter table public.sesi_mengajar
      add constraint sesi_mengajar_status_check check (status in ('draft', 'saved'));
  end if;
end;
$$;

create unique index if not exists sesi_mengajar_session_key
  on public.sesi_mengajar (distribusi_id, lesson_slot_id, tanggal);

create index if not exists sesi_mengajar_tenant_date_idx
  on public.sesi_mengajar (tenant_id, tanggal);

create index if not exists sesi_mengajar_distribusi_date_idx
  on public.sesi_mengajar (distribusi_id, tanggal);

alter table public.sesi_mengajar enable row level security;
grant select, insert, update, delete on table public.sesi_mengajar to authenticated;
revoke all on table public.sesi_mengajar from anon;

drop policy if exists sesi_mengajar_tenant_allow on public.sesi_mengajar;
create policy sesi_mengajar_tenant_allow
on public.sesi_mengajar
as permissive
for all
to authenticated
using (tenant_id = (select private.current_tenant_id()))
with check (tenant_id = (select private.current_tenant_id()));

drop policy if exists sesi_mengajar_tenant_guard on public.sesi_mengajar;
create policy sesi_mengajar_tenant_guard
on public.sesi_mengajar
as restrictive
for all
to authenticated
using (tenant_id = (select private.current_tenant_id()))
with check (tenant_id = (select private.current_tenant_id()));

drop trigger if exists enforce_tenant_sesi_mengajar on public.sesi_mengajar;
create trigger enforce_tenant_sesi_mengajar
before insert or update on public.sesi_mengajar
for each row execute function private.enforce_authenticated_row_tenant();

drop trigger if exists set_updated_at_sesi_mengajar on public.sesi_mengajar;
create trigger set_updated_at_sesi_mengajar
before update on public.sesi_mengajar
for each row execute function private.set_updated_at();

do $$
begin
  if to_regprocedure('private.current_session_satisfies_mfa()') is not null then
    drop policy if exists sesi_mengajar_mfa_guard on public.sesi_mengajar;
    create policy sesi_mengajar_mfa_guard
    on public.sesi_mengajar
    as restrictive
    for all
    to authenticated
    using ((select private.current_session_satisfies_mfa()))
    with check ((select private.current_session_satisfies_mfa()));
  end if;
end;
$$;

comment on table public.sesi_mengajar is
  'Metadata sesi mengajar dari jadwal guru: materi, isi pembelajaran, tugas/proyek, status draft/tersimpan, dan ringkasan penilaian.';

commit;
