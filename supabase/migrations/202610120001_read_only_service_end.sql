begin;

-- The application is retained as a historical archive after 11 October 2026.
-- Keep every existing record readable, but reject every data mutation at the
-- database boundary so legacy Android/web clients cannot bypass the closure.
create or replace function private.reject_writes_after_service_end()
returns trigger
language plpgsql
set search_path = pg_catalog
as $$
begin
  raise exception 'Layanan aktif MIM App telah berakhir. Data hanya dapat dilihat dan laporan data lama tetap dapat dibuat.'
    using errcode = '55000';
end;
$$;

revoke all on function private.reject_writes_after_service_end() from public;

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
    execute format(
      'create trigger app_read_only_after_service_end before insert or update or delete on public.%I for each row execute function private.reject_writes_after_service_end()',
      table_record.table_name
    );
  end loop;
end;
$$;

-- Existing attachments can still be downloaded for historical reports. New
-- uploads, replacements, and deletions are blocked with the same message.
drop trigger if exists app_read_only_after_service_end on storage.objects;
create trigger app_read_only_after_service_end
before insert or update or delete on storage.objects
for each row execute function private.reject_writes_after_service_end();

commit;
