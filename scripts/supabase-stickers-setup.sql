-- Supabase Storage bucket setup for chat stickers.
-- Tenant policies are owned by migration 202607010005. Do not restore the old
-- broad policies because they allow users to modify another unit's files.

-- Create bucket (or ensure it exists and public).
insert into storage.buckets (id, name, public)
values ('chat-stickers', 'chat-stickers', true)
on conflict (id) do nothing;

-- Remove the obsolete pre-Auth policies if this helper is run manually.
drop policy if exists "chat_stickers_select" on storage.objects;
drop policy if exists "chat_stickers_insert" on storage.objects;
drop policy if exists "chat_stickers_delete" on storage.objects;
