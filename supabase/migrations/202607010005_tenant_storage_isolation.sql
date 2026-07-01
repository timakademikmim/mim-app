begin;

-- New uploads use <tenant_id>/... as their first path segment. Public bucket
-- URLs stay compatible during rollout, while Storage writes and listing are
-- isolated by the active employee tenant resolved from auth.uid().
create or replace function private.storage_path_tenant_id(object_name text)
returns uuid
language plpgsql
immutable
set search_path = ''
as $$
declare
  v_first_segment text := split_part(ltrim(coalesce(object_name, ''), '/'), '/', 1);
begin
  if v_first_segment ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$' then
    return v_first_segment::uuid;
  end if;
  return null;
exception
  when invalid_text_representation then
    return null;
end;
$$;

create or replace function private.storage_object_is_current_tenant(object_name text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select private.storage_path_tenant_id(object_name) = private.current_tenant_id()
$$;

create or replace function private.storage_object_is_legacy(object_name text)
returns boolean
language sql
immutable
set search_path = ''
as $$
  select private.storage_path_tenant_id(object_name) is null
$$;

revoke all on function private.storage_path_tenant_id(text) from public;
revoke all on function private.storage_object_is_current_tenant(text) from public;
revoke all on function private.storage_object_is_legacy(text) from public;
grant usage on schema private to anon, authenticated;
grant execute on function private.storage_path_tenant_id(text) to anon, authenticated;
grant execute on function private.storage_object_is_current_tenant(text) to authenticated;
grant execute on function private.storage_object_is_legacy(text) to anon;

-- Keep current bucket visibility unchanged because report links are shared to
-- guardians. This migration secures mutation and listing; private/signed URLs
-- can be introduced separately after stored public URLs have been migrated.
insert into storage.buckets (id, name, public)
values
  ('karyawan-foto', 'karyawan-foto', true),
  ('surat-pemberitahuan', 'surat-pemberitahuan', true),
  ('soal-ujian-media', 'soal-ujian-media', true),
  ('laporan-bulanan', 'laporan-bulanan', true),
  ('laporan-uts', 'laporan-uts', true),
  ('chat-stickers', 'chat-stickers', true)
on conflict (id) do nothing;

-- Remove every pre-existing policy that explicitly references one of these
-- buckets. Leaving an older permissive policy in place would bypass isolation.
do $$
declare
  v_policy record;
  v_expression text;
  v_bucket text;
  v_managed_buckets text[] := array[
    'karyawan-foto',
    'surat-pemberitahuan',
    'soal-ujian-media',
    'laporan-bulanan',
    'laporan-uts',
    'chat-stickers'
  ];
begin
  for v_policy in
    select policy_row.polname, policy_row.polqual, policy_row.polwithcheck
    from pg_policy policy_row
    where policy_row.polrelid = 'storage.objects'::regclass
  loop
    v_expression := coalesce(pg_get_expr(v_policy.polqual, 'storage.objects'::regclass), '')
      || ' '
      || coalesce(pg_get_expr(v_policy.polwithcheck, 'storage.objects'::regclass), '');
    foreach v_bucket in array v_managed_buckets
    loop
      if position(v_bucket in v_expression) > 0 then
        execute format('drop policy if exists %I on storage.objects', v_policy.polname);
        exit;
      end if;
    end loop;
  end loop;
end;
$$;

drop policy if exists tenant_storage_select on storage.objects;
create policy tenant_storage_select
on storage.objects
for select
to authenticated
using (
  bucket_id = any (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  and private.storage_object_is_current_tenant(name)
);

drop policy if exists tenant_storage_insert on storage.objects;
create policy tenant_storage_insert
on storage.objects
for insert
to authenticated
with check (
  bucket_id = any (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  and private.storage_object_is_current_tenant(name)
);

drop policy if exists tenant_storage_update on storage.objects;
create policy tenant_storage_update
on storage.objects
for update
to authenticated
using (
  bucket_id = any (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  and private.storage_object_is_current_tenant(name)
)
with check (
  bucket_id = any (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  and private.storage_object_is_current_tenant(name)
);

drop policy if exists tenant_storage_delete on storage.objects;
create policy tenant_storage_delete
on storage.objects
for delete
to authenticated
using (
  bucket_id = any (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  and private.storage_object_is_current_tenant(name)
);

-- Restrictive guards make the tenant boundary survive any unrelated broad
-- permissive policy that may already exist on storage.objects.
drop policy if exists tenant_storage_authenticated_guard on storage.objects;
create policy tenant_storage_authenticated_guard
on storage.objects
as restrictive
for all
to authenticated
using (
  bucket_id <> all (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  or private.storage_object_is_current_tenant(name)
)
with check (
  bucket_id <> all (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  or private.storage_object_is_current_tenant(name)
);

-- Temporary bridge for clients that have not yet moved to Supabase Auth.
-- It can only touch old paths whose first segment is not a tenant UUID, so it
-- cannot bypass the authenticated tenant namespace above.
drop policy if exists legacy_storage_select_bridge on storage.objects;
create policy legacy_storage_select_bridge
on storage.objects
for select
to anon
using (
  bucket_id = any (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  and private.storage_object_is_legacy(name)
);

drop policy if exists legacy_storage_insert_bridge on storage.objects;
create policy legacy_storage_insert_bridge
on storage.objects
for insert
to anon
with check (
  bucket_id = any (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  and private.storage_object_is_legacy(name)
);

drop policy if exists legacy_storage_update_bridge on storage.objects;
create policy legacy_storage_update_bridge
on storage.objects
for update
to anon
using (
  bucket_id = any (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  and private.storage_object_is_legacy(name)
)
with check (
  bucket_id = any (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  and private.storage_object_is_legacy(name)
);

drop policy if exists legacy_storage_delete_bridge on storage.objects;
create policy legacy_storage_delete_bridge
on storage.objects
for delete
to anon
using (
  bucket_id = any (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  and private.storage_object_is_legacy(name)
);

drop policy if exists legacy_storage_anon_guard on storage.objects;
create policy legacy_storage_anon_guard
on storage.objects
as restrictive
for all
to anon
using (
  bucket_id <> all (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  or private.storage_object_is_legacy(name)
)
with check (
  bucket_id <> all (array[
    'karyawan-foto', 'surat-pemberitahuan', 'soal-ujian-media',
    'laporan-bulanan', 'laporan-uts', 'chat-stickers'
  ]::text[])
  or private.storage_object_is_legacy(name)
);

comment on function private.storage_object_is_current_tenant(text) is
  'Allows Storage access only when the first object path segment matches the active employee tenant.';

commit;
