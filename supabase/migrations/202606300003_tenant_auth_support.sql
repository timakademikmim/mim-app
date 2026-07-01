begin;

-- Employee login ids are unique inside a tenant, not across the organization.
do $$
declare
  v_constraint record;
  v_index record;
begin
  for v_constraint in
    select constraint_row.conname
    from pg_constraint constraint_row
    where constraint_row.conrelid = 'public.karyawan'::regclass
      and constraint_row.contype = 'u'
      and (
        select array_agg(attribute_row.attname order by key_row.ordinality)
        from unnest(constraint_row.conkey) with ordinality key_row(attnum, ordinality)
        join pg_attribute attribute_row
          on attribute_row.attrelid = constraint_row.conrelid
         and attribute_row.attnum = key_row.attnum
      ) = array['id_karyawan']::name[]
  loop
    execute format(
      'alter table public.karyawan drop constraint %I',
      v_constraint.conname
    );
  end loop;

  for v_index in
    select index_class.relname as index_name
    from pg_index index_row
    join pg_class index_class on index_class.oid = index_row.indexrelid
    where index_row.indrelid = 'public.karyawan'::regclass
      and index_row.indisunique
      and not index_row.indisprimary
      and index_row.indnkeyatts = 1
      and not exists (
        select 1
        from pg_constraint constraint_row
        where constraint_row.conindid = index_row.indexrelid
      )
      and (
        select attribute_row.attname
        from pg_attribute attribute_row
        where attribute_row.attrelid = index_row.indrelid
          and attribute_row.attnum = index_row.indkey[0]
      ) = 'id_karyawan'
  loop
    execute format('drop index public.%I', v_index.index_name);
  end loop;
end;
$$;

create unique index if not exists karyawan_tenant_login_unique_idx
  on public.karyawan (tenant_id, lower(btrim(id_karyawan)));

create table if not exists public.tenant_audit_logs (
  id uuid primary key default gen_random_uuid(),
  organization_id uuid not null references public.organizations(id) on delete restrict,
  tenant_id uuid null references public.tenants(id) on delete restrict,
  actor_auth_user_id uuid not null references auth.users(id) on delete restrict,
  actor_employee_id uuid null references public.karyawan(id) on delete set null,
  action text not null,
  target_type text not null,
  target_id text null,
  metadata_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  constraint tenant_audit_logs_action_not_blank check (btrim(action) <> ''),
  constraint tenant_audit_logs_target_type_not_blank check (btrim(target_type) <> '')
);

create index if not exists tenant_audit_logs_tenant_created_idx
  on public.tenant_audit_logs (tenant_id, created_at desc);

create index if not exists tenant_audit_logs_actor_created_idx
  on public.tenant_audit_logs (actor_auth_user_id, created_at desc);

alter table public.tenant_audit_logs enable row level security;
revoke all on table public.tenant_audit_logs from anon, authenticated;

comment on table public.tenant_audit_logs is
  'Server-written audit trail for platform and tenant administration actions.';

commit;
