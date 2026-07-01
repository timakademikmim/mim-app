begin;

grant usage on schema private to authenticated;
grant execute on function private.storage_object_is_legacy(text) to authenticated;

commit;
