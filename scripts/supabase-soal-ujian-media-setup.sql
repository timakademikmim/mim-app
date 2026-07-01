-- Supabase Storage bucket setup for exam question images.
-- Tenant policies are owned by migration 202607010005. Do not add a broad
-- anon policy here because it would bypass cross-tenant isolation.

-- Create bucket (or ensure it exists and public).
insert into storage.buckets (id, name, public)
values ('soal-ujian-media', 'soal-ujian-media', true)
on conflict (id) do nothing;

-- Remove the obsolete pre-Auth policies if this helper is run manually.
drop policy if exists "soal_ujian_media_select" on storage.objects;
drop policy if exists "soal_ujian_media_insert" on storage.objects;
drop policy if exists "soal_ujian_media_update" on storage.objects;
drop policy if exists "soal_ujian_media_delete" on storage.objects;
