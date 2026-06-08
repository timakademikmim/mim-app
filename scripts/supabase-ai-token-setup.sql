create extension if not exists "pgcrypto";

create table if not exists public.ai_token_wallets (
  guru_id text primary key,
  balance_tokens integer not null default 0 check (balance_tokens >= 0),
  total_purchased_tokens integer not null default 0 check (total_purchased_tokens >= 0),
  total_used_tokens integer not null default 0 check (total_used_tokens >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.ai_token_ledger (
  id uuid primary key default gen_random_uuid(),
  guru_id text not null references public.ai_token_wallets(guru_id) on delete cascade,
  delta_tokens integer not null,
  balance_after integer not null,
  reason text not null,
  ref_type text null,
  ref_id text null,
  metadata_json jsonb not null default '{}'::jsonb,
  created_by text null,
  created_at timestamptz not null default now()
);

create index if not exists ai_token_ledger_guru_created_idx
  on public.ai_token_ledger (guru_id, created_at desc);

create table if not exists public.ai_usage_logs (
  id uuid primary key default gen_random_uuid(),
  guru_id text not null,
  feature text not null,
  distribusi_id text null,
  model text null,
  prompt_tokens integer not null default 0,
  completion_tokens integer not null default 0,
  total_tokens integer not null default 0,
  reserved_tokens integer not null default 0,
  charged_tokens integer not null default 0,
  status text not null default 'success',
  error_message text null,
  metadata_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists ai_usage_logs_guru_created_idx
  on public.ai_usage_logs (guru_id, created_at desc);

create or replace function public.set_ai_token_wallets_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_ai_token_wallets_updated_at
  on public.ai_token_wallets;

create trigger trg_ai_token_wallets_updated_at
before update on public.ai_token_wallets
for each row
execute function public.set_ai_token_wallets_updated_at();

create or replace function public.ai_get_or_create_wallet(p_guru_id text)
returns public.ai_token_wallets
language plpgsql
security definer
set search_path = public
as $$
declare
  v_wallet public.ai_token_wallets;
begin
  if nullif(trim(p_guru_id), '') is null then
    raise exception 'guru_id wajib diisi';
  end if;

  insert into public.ai_token_wallets (guru_id)
  values (trim(p_guru_id))
  on conflict (guru_id) do nothing;

  select *
    into v_wallet
    from public.ai_token_wallets
   where guru_id = trim(p_guru_id);

  return v_wallet;
end;
$$;

create or replace function public.ai_credit_tokens(
  p_guru_id text,
  p_amount integer,
  p_reason text default 'purchase',
  p_ref_type text default null,
  p_ref_id text default null,
  p_metadata_json jsonb default '{}'::jsonb,
  p_created_by text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_guru_id text := trim(coalesce(p_guru_id, ''));
  v_wallet public.ai_token_wallets;
  v_amount integer := coalesce(p_amount, 0);
begin
  if v_guru_id = '' then
    return jsonb_build_object('ok', false, 'error', 'guru_id wajib diisi');
  end if;

  if v_amount <= 0 then
    return jsonb_build_object('ok', false, 'error', 'amount token harus lebih dari 0');
  end if;

  perform public.ai_get_or_create_wallet(v_guru_id);

  update public.ai_token_wallets
     set balance_tokens = balance_tokens + v_amount,
         total_purchased_tokens = total_purchased_tokens + case when coalesce(p_reason, '') = 'purchase' then v_amount else 0 end,
         total_used_tokens = case
           when coalesce(p_reason, '') in ('refund', 'usage_refund') then greatest(0, total_used_tokens - v_amount)
           else total_used_tokens
         end
   where guru_id = v_guru_id
   returning *
    into v_wallet;

  insert into public.ai_token_ledger (
    guru_id,
    delta_tokens,
    balance_after,
    reason,
    ref_type,
    ref_id,
    metadata_json,
    created_by
  ) values (
    v_guru_id,
    v_amount,
    v_wallet.balance_tokens,
    coalesce(nullif(trim(p_reason), ''), 'credit'),
    p_ref_type,
    p_ref_id,
    coalesce(p_metadata_json, '{}'::jsonb),
    p_created_by
  );

  return jsonb_build_object(
    'ok', true,
    'guru_id', v_wallet.guru_id,
    'balance_tokens', v_wallet.balance_tokens,
    'total_purchased_tokens', v_wallet.total_purchased_tokens,
    'total_used_tokens', v_wallet.total_used_tokens
  );
end;
$$;

create or replace function public.ai_debit_tokens(
  p_guru_id text,
  p_amount integer,
  p_reason text default 'usage',
  p_ref_type text default null,
  p_ref_id text default null,
  p_metadata_json jsonb default '{}'::jsonb,
  p_created_by text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_guru_id text := trim(coalesce(p_guru_id, ''));
  v_wallet public.ai_token_wallets;
  v_amount integer := coalesce(p_amount, 0);
begin
  if v_guru_id = '' then
    return jsonb_build_object('ok', false, 'error', 'guru_id wajib diisi');
  end if;

  if v_amount <= 0 then
    return jsonb_build_object('ok', false, 'error', 'amount token harus lebih dari 0');
  end if;

  perform public.ai_get_or_create_wallet(v_guru_id);

  select *
    into v_wallet
    from public.ai_token_wallets
   where guru_id = v_guru_id
   for update;

  if v_wallet.balance_tokens < v_amount then
    return jsonb_build_object(
      'ok', false,
      'error', 'Saldo token AI tidak cukup',
      'guru_id', v_wallet.guru_id,
      'balance_tokens', v_wallet.balance_tokens,
      'required_tokens', v_amount
    );
  end if;

  update public.ai_token_wallets
     set balance_tokens = balance_tokens - v_amount,
         total_used_tokens = total_used_tokens + case when coalesce(p_reason, '') in ('usage', 'usage_adjustment') then v_amount else 0 end
   where guru_id = v_guru_id
   returning *
    into v_wallet;

  insert into public.ai_token_ledger (
    guru_id,
    delta_tokens,
    balance_after,
    reason,
    ref_type,
    ref_id,
    metadata_json,
    created_by
  ) values (
    v_guru_id,
    -v_amount,
    v_wallet.balance_tokens,
    coalesce(nullif(trim(p_reason), ''), 'usage'),
    p_ref_type,
    p_ref_id,
    coalesce(p_metadata_json, '{}'::jsonb),
    p_created_by
  );

  return jsonb_build_object(
    'ok', true,
    'guru_id', v_wallet.guru_id,
    'balance_tokens', v_wallet.balance_tokens,
    'total_purchased_tokens', v_wallet.total_purchased_tokens,
    'total_used_tokens', v_wallet.total_used_tokens
  );
end;
$$;

alter table public.ai_token_wallets enable row level security;
alter table public.ai_token_ledger enable row level security;
alter table public.ai_usage_logs enable row level security;

revoke all on public.ai_token_wallets from anon, authenticated;
revoke all on public.ai_token_ledger from anon, authenticated;
revoke all on public.ai_usage_logs from anon, authenticated;

revoke execute on function public.ai_get_or_create_wallet(text) from public, anon, authenticated;
revoke execute on function public.ai_credit_tokens(text, integer, text, text, text, jsonb, text) from public, anon, authenticated;
revoke execute on function public.ai_debit_tokens(text, integer, text, text, text, jsonb, text) from public, anon, authenticated;

grant execute on function public.ai_get_or_create_wallet(text) to service_role;
grant execute on function public.ai_credit_tokens(text, integer, text, text, text, jsonb, text) to service_role;
grant execute on function public.ai_debit_tokens(text, integer, text, text, text, jsonb, text) to service_role;
