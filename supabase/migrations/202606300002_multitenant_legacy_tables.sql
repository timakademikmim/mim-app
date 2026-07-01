begin;

-- These legacy academic tables were discovered from the production catalog
-- after the initial foundation migration. AI-specific tables remain excluded.
do $$
declare
  v_tenant_id uuid;
  v_table text;
  v_constraint_name text;
  v_tables text[] := array[
    'wali_santri',
    'guru_detail',
    'tahun_angkatan',
    'penilaian_musyrif',
    'orang_tua',
    'nilai_tahfiz',
    'guru_mapel',
    'kehadiran_guru'
  ];
begin
  select tenant.id
  into v_tenant_id
  from public.tenants tenant
  join public.organizations organization
    on organization.id = tenant.organization_id
  where lower(organization.code) = 'mim'
    and lower(tenant.code) = 'putra'
  limit 1;

  if v_tenant_id is null then
    raise exception 'Putra tenant was not found';
  end if;

  foreach v_table in array v_tables loop
    if to_regclass(format('public.%I', v_table)) is null then
      continue;
    end if;

    execute format(
      'alter table public.%I add column if not exists tenant_id uuid',
      v_table
    );
    execute format(
      'update public.%I set tenant_id = $1 where tenant_id is null',
      v_table
    ) using v_tenant_id;
    execute format(
      'alter table public.%I alter column tenant_id set default %L::uuid',
      v_table,
      v_tenant_id
    );
    execute format(
      'alter table public.%I alter column tenant_id set not null',
      v_table
    );
    execute format(
      'create index if not exists %I on public.%I (tenant_id)',
      left('idx_' || v_table || '_tenant', 63),
      v_table
    );

    v_constraint_name := left(
      'fk_' || v_table || '_tenant_' || substr(md5(v_table), 1, 8),
      63
    );

    if not exists (
      select 1
      from pg_constraint
      where conrelid = format('public.%I', v_table)::regclass
        and conname = v_constraint_name
    ) then
      execute format(
        'alter table public.%I add constraint %I foreign key (tenant_id) references public.tenants(id) on delete restrict not valid',
        v_table,
        v_constraint_name
      );
    end if;

    execute format(
      'alter table public.%I validate constraint %I',
      v_table,
      v_constraint_name
    );
  end loop;
end;
$$;

commit;
