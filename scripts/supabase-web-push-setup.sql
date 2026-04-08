create extension if not exists "pgcrypto";

create table if not exists public.web_push_subscriptions (
  id uuid primary key default gen_random_uuid(),
  user_id text not null,
  endpoint text not null,
  p256dh text not null,
  auth text not null,
  role text null,
  user_agent text null,
  device_id text null,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now()
);

create unique index if not exists web_push_subscriptions_endpoint_key
  on public.web_push_subscriptions (endpoint);

create index if not exists web_push_subscriptions_user_id_idx
  on public.web_push_subscriptions (user_id);

create index if not exists web_push_subscriptions_user_active_idx
  on public.web_push_subscriptions (user_id, is_active);

create table if not exists public.notification_events (
  id uuid primary key default gen_random_uuid(),
  user_id text not null,
  scope text not null default 'general',
  event_type text not null default 'info',
  title text not null,
  body text null,
  route text null,
  thread_id text null,
  payload_json jsonb not null default '{}'::jsonb,
  source_user_id text null,
  dedupe_key text null,
  created_at timestamptz not null default now(),
  read_at timestamptz null
);

create unique index if not exists notification_events_user_dedupe_key
  on public.notification_events (user_id, dedupe_key)
  where dedupe_key is not null;

create index if not exists notification_events_user_created_at_idx
  on public.notification_events (user_id, created_at desc);

create or replace function public.set_web_push_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_web_push_subscriptions_updated_at
  on public.web_push_subscriptions;

create trigger trg_web_push_subscriptions_updated_at
before update on public.web_push_subscriptions
for each row
execute function public.set_web_push_updated_at();
