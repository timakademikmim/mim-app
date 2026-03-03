-- Chat schema (DM + Group) with 24-hour retention
-- Run in Supabase SQL Editor.

create extension if not exists pgcrypto;

create table if not exists public.chat_threads (
  id uuid primary key default gen_random_uuid(),
  title text,
  is_group boolean not null default false,
  created_by text,
  created_at timestamptz not null default now(),
  last_message_at timestamptz
);

create table if not exists public.chat_thread_members (
  id bigint generated always as identity primary key,
  thread_id uuid not null references public.chat_threads(id) on delete cascade,
  karyawan_id text not null,
  joined_at timestamptz not null default now(),
  unique (thread_id, karyawan_id)
);

create table if not exists public.chat_messages (
  id bigint generated always as identity primary key,
  thread_id uuid not null references public.chat_threads(id) on delete cascade,
  sender_id text not null,
  message_text text not null,
  created_at timestamptz not null default now()
);

create index if not exists idx_chat_thread_members_karyawan on public.chat_thread_members(karyawan_id);
create index if not exists idx_chat_messages_thread_created on public.chat_messages(thread_id, created_at desc);
create index if not exists idx_chat_messages_created on public.chat_messages(created_at);
create index if not exists idx_chat_threads_last_message on public.chat_threads(last_message_at desc);

alter table public.chat_threads enable row level security;
alter table public.chat_thread_members enable row level security;
alter table public.chat_messages enable row level security;

-- App ini menggunakan login internal (bukan Supabase Auth JWT),
-- jadi policy dibuat permissive agar fitur tetap berjalan.
drop policy if exists chat_threads_all_anon on public.chat_threads;
create policy chat_threads_all_anon on public.chat_threads for all to anon using (true) with check (true);
drop policy if exists chat_members_all_anon on public.chat_thread_members;
create policy chat_members_all_anon on public.chat_thread_members for all to anon using (true) with check (true);
drop policy if exists chat_messages_all_anon on public.chat_messages;
create policy chat_messages_all_anon on public.chat_messages for all to anon using (true) with check (true);

-- Optional automatic cleanup via pg_cron (recommended)
-- Enable pg_cron in project if available.
do $$
begin
  begin
    perform cron.unschedule('chat_cleanup_24h');
  exception when others then
    null;
  end;
  perform cron.schedule(
    'chat_cleanup_24h',
    '*/15 * * * *',
    $$delete from public.chat_messages where created_at < now() - interval '24 hours'$$
  );
exception when undefined_function then
  null;
end $$;
