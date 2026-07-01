-- Read-only checks after applying 202607010005_tenant_storage_isolation.sql.

select
  bucket_row.id,
  bucket_row.public,
  bucket_row.file_size_limit,
  bucket_row.allowed_mime_types
from storage.buckets bucket_row
where bucket_row.id = any (array[
  'karyawan-foto',
  'surat-pemberitahuan',
  'soal-ujian-media',
  'laporan-bulanan',
  'laporan-uts',
  'chat-stickers'
]::text[])
order by bucket_row.id;

select
  policy_row.policyname,
  policy_row.permissive,
  policy_row.roles,
  policy_row.cmd,
  policy_row.qual,
  policy_row.with_check
from pg_policies policy_row
where policy_row.schemaname = 'storage'
  and policy_row.tablename = 'objects'
  and policy_row.policyname in (
    'tenant_storage_select',
    'tenant_storage_insert',
    'tenant_storage_update',
    'tenant_storage_delete',
    'tenant_storage_authenticated_guard',
    'legacy_storage_select_bridge',
    'legacy_storage_insert_bridge',
    'legacy_storage_update_bridge',
    'legacy_storage_delete_bridge',
    'legacy_storage_anon_guard'
  )
order by policy_row.policyname;

do $$
declare
  v_tenant uuid;
begin
  select tenant_row.id
  into v_tenant
  from public.tenants tenant_row
  order by tenant_row.created_at
  limit 1;
  assert v_tenant is not null, 'no tenant is available for Storage verification';
  assert private.storage_path_tenant_id(v_tenant::text || '/folder/file.pdf') = v_tenant,
    'tenant prefix parser failed';
  assert private.storage_object_is_legacy('users/employee/file.webp'),
    'legacy path detection failed';
  assert not private.storage_object_is_legacy(v_tenant::text || '/folder/file.pdf'),
    'tenant path was incorrectly treated as legacy';
end;
$$;

select
  count(*) as total_accounts,
  count(*) filter (where auth_user_id is not null) as auth_accounts,
  count(*) filter (where auth_user_id is null) as legacy_accounts,
  count(*) filter (where aktif is true and auth_user_id is null) as active_legacy_accounts
from public.karyawan;
