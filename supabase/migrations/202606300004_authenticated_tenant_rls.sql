begin;

-- Authenticated writes never trust tenant_id supplied by Android or web.
-- Service-role and temporary anonymous compatibility requests are left alone.
create or replace function private.enforce_authenticated_row_tenant()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_auth_user_id uuid := (select auth.uid());
  v_tenant_id uuid;
begin
  if v_auth_user_id is null then
    return new;
  end if;

  v_tenant_id := (select private.current_tenant_id());
  if v_tenant_id is null then
    raise exception 'Authenticated account has no active tenant'
      using errcode = '42501';
  end if;

  if tg_op = 'UPDATE' and old.tenant_id is distinct from v_tenant_id then
    raise exception 'Cross-tenant update is not allowed'
      using errcode = '42501';
  end if;

  new.tenant_id := v_tenant_id;
  return new;
end;
$$;

revoke all on function private.enforce_authenticated_row_tenant() from public;

-- Apply a permissive tenant policy plus a restrictive guard. The restrictive
-- policy keeps authenticated users isolated even if an older broad policy is
-- still present. The anon bridge is temporary and removed at final cutover.
do $$
declare
  v_table record;
  v_policy_prefix text;
  v_trigger_name text;
begin
  for v_table in
    select distinct column_row.table_name
    from information_schema.columns column_row
    join information_schema.tables table_row
      on table_row.table_schema = column_row.table_schema
     and table_row.table_name = column_row.table_name
    where column_row.table_schema = 'public'
      and column_row.column_name = 'tenant_id'
      and table_row.table_type = 'BASE TABLE'
      and column_row.table_name not in ('karyawan', 'tenant_audit_logs')
      and column_row.table_name not like 'ai\_%' escape '\'
      and column_row.table_name not like 'chatgpt\_%' escape '\'
    order by column_row.table_name
  loop
    v_policy_prefix := left(
      'tenant_' || v_table.table_name || '_' || substr(md5(v_table.table_name), 1, 8),
      48
    );
    v_trigger_name := left(
      'enforce_tenant_' || v_table.table_name || '_' || substr(md5(v_table.table_name), 1, 8),
      63
    );

    execute format('alter table public.%I enable row level security', v_table.table_name);
    execute format(
      'grant select, insert, update, delete on table public.%I to authenticated',
      v_table.table_name
    );

    execute format('drop policy if exists %I on public.%I', v_policy_prefix || '_allow', v_table.table_name);
    execute format(
      'create policy %I on public.%I as permissive for all to authenticated using (tenant_id = (select private.current_tenant_id())) with check (tenant_id = (select private.current_tenant_id()))',
      v_policy_prefix || '_allow',
      v_table.table_name
    );

    execute format('drop policy if exists %I on public.%I', v_policy_prefix || '_guard', v_table.table_name);
    execute format(
      'create policy %I on public.%I as restrictive for all to authenticated using (tenant_id = (select private.current_tenant_id())) with check (tenant_id = (select private.current_tenant_id()))',
      v_policy_prefix || '_guard',
      v_table.table_name
    );

    execute format('drop policy if exists %I on public.%I', v_policy_prefix || '_anon_bridge', v_table.table_name);
    execute format(
      'create policy %I on public.%I as permissive for all to anon using (true) with check (true)',
      v_policy_prefix || '_anon_bridge',
      v_table.table_name
    );

    execute format('drop trigger if exists %I on public.%I', v_trigger_name, v_table.table_name);
    execute format(
      'create trigger %I before insert or update on public.%I for each row execute function private.enforce_authenticated_row_tenant()',
      v_trigger_name,
      v_table.table_name
    );
  end loop;
end;
$$;

-- Employee data is more sensitive than general academic rows. All unit users
-- may read the directory, but direct writes require the admin role. Self-service
-- profile and password changes run through manage-self-profile instead.
alter table public.karyawan enable row level security;
grant select, insert, update, delete on table public.karyawan to authenticated;

drop policy if exists karyawan_tenant_select_allow on public.karyawan;
create policy karyawan_tenant_select_allow
on public.karyawan as permissive
for select to authenticated
using (tenant_id = (select private.current_tenant_id()));

drop policy if exists karyawan_tenant_select_guard on public.karyawan;
create policy karyawan_tenant_select_guard
on public.karyawan as restrictive
for select to authenticated
using (tenant_id = (select private.current_tenant_id()));

drop policy if exists karyawan_tenant_insert_allow on public.karyawan;
create policy karyawan_tenant_insert_allow
on public.karyawan as permissive
for insert to authenticated
with check (
  tenant_id = (select private.current_tenant_id())
  and (select private.current_user_has_role('admin'))
);

drop policy if exists karyawan_tenant_insert_guard on public.karyawan;
create policy karyawan_tenant_insert_guard
on public.karyawan as restrictive
for insert to authenticated
with check (
  tenant_id = (select private.current_tenant_id())
  and (select private.current_user_has_role('admin'))
);

drop policy if exists karyawan_tenant_update_allow on public.karyawan;
create policy karyawan_tenant_update_allow
on public.karyawan as permissive
for update to authenticated
using (
  tenant_id = (select private.current_tenant_id())
  and (select private.current_user_has_role('admin'))
)
with check (
  tenant_id = (select private.current_tenant_id())
  and (select private.current_user_has_role('admin'))
);

drop policy if exists karyawan_tenant_update_guard on public.karyawan;
create policy karyawan_tenant_update_guard
on public.karyawan as restrictive
for update to authenticated
using (
  tenant_id = (select private.current_tenant_id())
  and (select private.current_user_has_role('admin'))
)
with check (
  tenant_id = (select private.current_tenant_id())
  and (select private.current_user_has_role('admin'))
);

drop policy if exists karyawan_tenant_delete_allow on public.karyawan;
create policy karyawan_tenant_delete_allow
on public.karyawan as permissive
for delete to authenticated
using (
  tenant_id = (select private.current_tenant_id())
  and (select private.current_user_has_role('admin'))
);

drop policy if exists karyawan_tenant_delete_guard on public.karyawan;
create policy karyawan_tenant_delete_guard
on public.karyawan as restrictive
for delete to authenticated
using (
  tenant_id = (select private.current_tenant_id())
  and (select private.current_user_has_role('admin'))
);

drop policy if exists karyawan_legacy_anon_bridge on public.karyawan;
create policy karyawan_legacy_anon_bridge
on public.karyawan as permissive
for all to anon
using (true)
with check (true);

drop trigger if exists enforce_tenant_karyawan on public.karyawan;
create trigger enforce_tenant_karyawan
before insert or update on public.karyawan
for each row execute function private.enforce_authenticated_row_tenant();

comment on function private.enforce_authenticated_row_tenant() is
  'Forces authenticated inserts and updates to the active employee tenant. Anon behavior remains only for rollout compatibility.';

commit;
