-- Supabase Storage setup for chat stickers (quick/anon version).
-- This allows any anon client to list/upload/delete within the bucket.
-- Use only if you are not using Supabase Auth yet.

-- Create bucket (or ensure it exists and public).
insert into storage.buckets (id, name, public)
values ('chat-stickers', 'chat-stickers', true)
on conflict (id) do update set public = true;

-- Recreate policies for anon access.
drop policy if exists "chat_stickers_select" on storage.objects;
drop policy if exists "chat_stickers_insert" on storage.objects;
drop policy if exists "chat_stickers_delete" on storage.objects;

create policy "chat_stickers_select"
on storage.objects
for select
using (
  bucket_id = 'chat-stickers'
  and name like 'users/%'
);

create policy "chat_stickers_insert"
on storage.objects
for insert
with check (
  bucket_id = 'chat-stickers'
  and name like 'users/%'
);

create policy "chat_stickers_delete"
on storage.objects
for delete
using (
  bucket_id = 'chat-stickers'
  and name like 'users/%'
);

