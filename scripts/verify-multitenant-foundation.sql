-- Read-only checks to run after 202606300001_multitenant_foundation.sql.

select
  organization.code as organization_code,
  organization.name as organization_name,
  tenant.id as tenant_id,
  tenant.code as tenant_code,
  tenant.name as tenant_name,
  tenant.address,
  tenant.active
from public.organizations organization
join public.tenants tenant on tenant.organization_id = organization.id
order by organization.code, tenant.code;

select
  table_name,
  is_nullable,
  column_default
from information_schema.columns
where table_schema = 'public'
  and column_name = 'tenant_id'
order by table_name;

do $$
declare
  v_table record;
  v_null_count bigint;
begin
  for v_table in
    select table_name
    from information_schema.columns
    where table_schema = 'public'
      and column_name = 'tenant_id'
    order by table_name
  loop
    execute format(
      'select count(*) from public.%I where tenant_id is null',
      v_table.table_name
    ) into v_null_count;

    if v_null_count > 0 then
      raise warning 'Table % still has % rows without tenant_id',
        v_table.table_name,
        v_null_count;
    else
      raise notice 'Table %: tenant backfill OK', v_table.table_name;
    end if;
  end loop;
end;
$$;

select
  employee.tenant_id,
  count(*) as employee_count,
  count(*) filter (where employee.auth_user_id is not null) as auth_linked_count,
  count(*) filter (where employee.auth_user_id is null) as legacy_account_count
from public.karyawan employee
group by employee.tenant_id
order by employee.tenant_id;

-- Review global uniqueness before allowing the same employee login id in
-- different tenants. Do not drop constraints until their foreign keys and
-- dependent code have been checked.
select
  constraint_definition.conname as constraint_name,
  pg_get_constraintdef(constraint_definition.oid) as definition
from pg_constraint constraint_definition
where constraint_definition.conrelid = 'public.karyawan'::regclass
  and constraint_definition.contype in ('p', 'u', 'f', 'c')
order by constraint_definition.contype, constraint_definition.conname;
