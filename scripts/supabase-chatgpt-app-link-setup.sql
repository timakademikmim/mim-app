create extension if not exists "pgcrypto";

create table if not exists public.chatgpt_app_guru_links (
  id uuid primary key default gen_random_uuid(),
  provider text not null default 'chatgpt',
  external_subject text not null,
  guru_id uuid not null,
  guru_nama text null,
  display_name text null,
  email text null,
  metadata_json jsonb not null default '{}'::jsonb,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  last_seen_at timestamptz null
);

create unique index if not exists chatgpt_app_guru_links_provider_subject_key
  on public.chatgpt_app_guru_links (provider, external_subject);

create index if not exists chatgpt_app_guru_links_guru_id_idx
  on public.chatgpt_app_guru_links (guru_id);

create index if not exists chatgpt_app_guru_links_active_idx
  on public.chatgpt_app_guru_links (provider, is_active);

create or replace function public.set_chatgpt_app_guru_links_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_chatgpt_app_guru_links_updated_at
  on public.chatgpt_app_guru_links;

create trigger trg_chatgpt_app_guru_links_updated_at
before update on public.chatgpt_app_guru_links
for each row
execute function public.set_chatgpt_app_guru_links_updated_at();

alter table public.chatgpt_app_guru_links enable row level security;

drop policy if exists chatgpt_app_guru_links_all_anon on public.chatgpt_app_guru_links;
create policy chatgpt_app_guru_links_all_anon
on public.chatgpt_app_guru_links
for all
to anon
using (true)
with check (true);

drop policy if exists chatgpt_app_guru_links_all_authenticated on public.chatgpt_app_guru_links;
create policy chatgpt_app_guru_links_all_authenticated
on public.chatgpt_app_guru_links
for all
to authenticated
using (true)
with check (true);

create table if not exists public.chatgpt_app_link_codes (
  id uuid primary key default gen_random_uuid(),
  provider text not null default 'chatgpt',
  code text not null,
  guru_id uuid not null,
  guru_nama text null,
  expires_at timestamptz not null,
  used_at timestamptz null,
  used_by_subject text null,
  is_active boolean not null default true,
  metadata_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists chatgpt_app_link_codes_provider_code_key
  on public.chatgpt_app_link_codes (provider, code);

create index if not exists chatgpt_app_link_codes_guru_id_idx
  on public.chatgpt_app_link_codes (guru_id);

create index if not exists chatgpt_app_link_codes_active_idx
  on public.chatgpt_app_link_codes (provider, is_active, expires_at);

create or replace function public.set_chatgpt_app_link_codes_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_chatgpt_app_link_codes_updated_at
  on public.chatgpt_app_link_codes;

create trigger trg_chatgpt_app_link_codes_updated_at
before update on public.chatgpt_app_link_codes
for each row
execute function public.set_chatgpt_app_link_codes_updated_at();

alter table public.chatgpt_app_link_codes enable row level security;

drop policy if exists chatgpt_app_link_codes_all_anon on public.chatgpt_app_link_codes;
create policy chatgpt_app_link_codes_all_anon
on public.chatgpt_app_link_codes
for all
to anon
using (true)
with check (true);

drop policy if exists chatgpt_app_link_codes_all_authenticated on public.chatgpt_app_link_codes;
create policy chatgpt_app_link_codes_all_authenticated
on public.chatgpt_app_link_codes
for all
to authenticated
using (true)
with check (true);
