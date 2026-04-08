-- Supabase Storage setup for exam question images.
-- This project currently uploads from the browser using the anon client,
-- so the bucket and storage.objects policies must allow anon access.

-- Create bucket (or ensure it exists and public).
insert into storage.buckets (id, name, public)
values ('soal-ujian-media', 'soal-ujian-media', true)
on conflict (id) do update set public = true;

-- Recreate policies for anon access inside this bucket.
drop policy if exists "soal_ujian_media_select" on storage.objects;
drop policy if exists "soal_ujian_media_insert" on storage.objects;
drop policy if exists "soal_ujian_media_update" on storage.objects;
drop policy if exists "soal_ujian_media_delete" on storage.objects;

create policy "soal_ujian_media_select"
on storage.objects
for select
to anon
using (
  bucket_id = 'soal-ujian-media'
);

create policy "soal_ujian_media_insert"
on storage.objects
for insert
to anon
with check (
  bucket_id = 'soal-ujian-media'
);

create policy "soal_ujian_media_update"
on storage.objects
for update
to anon
using (
  bucket_id = 'soal-ujian-media'
)
with check (
  bucket_id = 'soal-ujian-media'
);

create policy "soal_ujian_media_delete"
on storage.objects
for delete
to anon
using (
  bucket_id = 'soal-ujian-media'
);
