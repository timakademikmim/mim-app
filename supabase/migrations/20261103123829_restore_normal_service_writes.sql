begin;

-- Restore normal service operations. This only removes the temporary
-- application-wide read-only guard installed at the end of service.
do $$
declare
  table_record record;
begin
  for table_record in
    select table_name
    from information_schema.tables
    where table_schema = 'public'
      and table_type = 'BASE TABLE'
  loop
    execute format(
      'drop trigger if exists app_read_only_after_service_end on public.%I',
      table_record.table_name
    );
  end loop;

  if to_regclass('storage.objects') is not null then
    execute 'drop trigger if exists app_read_only_after_service_end on storage.objects';
  end if;

  if exists (select 1 from pg_namespace where nspname = 'private') then
    execute 'drop function if exists private.reject_writes_after_service_end()';
  end if;
end;
$$;

commit;
