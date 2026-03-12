-- FCM push token storage
-- Jalankan di Supabase SQL Editor.

create extension if not exists "pgcrypto";

create table if not exists public.push_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id text not null,
  token text not null,
  platform text not null default 'android',
  device_id text,
  app_version text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists push_tokens_token_key on public.push_tokens(token);
create index if not exists push_tokens_user_id_idx on public.push_tokens(user_id);

create or replace function public.set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

drop trigger if exists set_push_tokens_updated_at on public.push_tokens;
create trigger set_push_tokens_updated_at
before update on public.push_tokens
for each row execute procedure public.set_updated_at();

-- Jika kamu pakai RLS, buka komentar di bawah:
-- alter table public.push_tokens enable row level security;
-- create policy "allow anon insert tokens"
--   on public.push_tokens
--   for insert
--   with check (true);
-- create policy "allow anon update tokens"
--   on public.push_tokens
--   for update
--   using (true);
-- create policy "allow anon select tokens"
--   on public.push_tokens
--   for select
--   using (true);
