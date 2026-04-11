# Linking Akun ChatGPT Ke Guru

Dokumen ini menyiapkan jalur agar nanti ChatGPT App tidak perlu lagi meminta `guru_id` manual.

## Tujuan

Saat guru menggunakan ChatGPT App:

1. ChatGPT App mengenali identitas user
2. sistem kita mencari link ke data guru
3. tool ujian memakai hasil link itu
4. guru tidak perlu mengetik `guru_id`

## Fondasi Yang Sudah Disiapkan Lokal

1. SQL tabel link:
   - [supabase-chatgpt-app-link-setup.sql](d:\004%20MA%20MIM\000%20App\login-supabase\scripts\supabase-chatgpt-app-link-setup.sql)
2. Function resolver:
   - [index.ts](d:\004%20MA%20MIM\000%20App\login-supabase\supabase\functions\resolve-chatgpt-app-guru\index.ts)
3. Contoh payload resolver:
   - [chatgpt-app-link-sample-request.json](d:\004%20MA%20MIM\000%20App\login-supabase\docs\chatgpt-app-link-sample-request.json)
4. Contoh payload tukar kode:
   - [chatgpt-app-link-code-sample-request.json](d:\004%20MA%20MIM\000%20App\login-supabase\docs\chatgpt-app-link-code-sample-request.json)

## Tabel Yang Direncanakan

`chatgpt_app_guru_links`

Field penting:

1. `provider`
2. `external_subject`
3. `guru_id`
4. `guru_nama`
5. `display_name`
6. `email`
7. `is_active`
8. `last_seen_at`

`chatgpt_app_link_codes`

Field penting:

1. `provider`
2. `code`
3. `guru_id`
4. `guru_nama`
5. `expires_at`
6. `used_at`
7. `used_by_subject`
8. `is_active`

## Alur Yang Direncanakan

1. User masuk ke ChatGPT App
2. App memperoleh identitas user dari platform
3. MCP/tool memanggil `resolve-chatgpt-app-guru`
4. Jika `status = linked`, tool ujian memakai `guru_id` itu
5. Jika `status = not_linked`, app meminta proses penautan dulu
6. Opsi penautan yang disarankan: user memasukkan `kode tautan sekali pakai`
7. App menukar kode itu lewat function `exchange-chatgpt-app-link-code`
8. Setelah berhasil, link permanen masuk ke `chatgpt_app_guru_links`

## Langkah Penautan Yang Direncanakan

1. admin membuka `Data Guru -> Detail -> ChatGPT`
2. admin membuat `kode tautan` untuk guru
3. user memasukkan kode itu di ChatGPT App
4. function `exchange-chatgpt-app-link-code` membuat link permanen
5. data link masuk ke `chatgpt_app_guru_links`
6. saat user membuka ChatGPT App berikutnya, identitas user dicek ke tabel link
7. jika cocok, semua tool ujian memakai `guru_id` hasil resolver itu

## Status Lokal Saat Ini

1. Tab admin `ChatGPT` di `Detail Guru` sudah ada secara lokal.
2. Admin juga bisa masuk cepat ke tab itu dari daftar guru.
3. Admin sekarang sudah bisa membuat `kode tautan 24 jam` secara lokal.
4. Jika tabel belum ada, UI menampilkan petunjuk SQL yang harus dijalankan.
5. MCP lokal sekarang sudah mendukung mode resolver bertahap: `local-only`, `local-first`, `live-first`, dan `live-only`.
6. Function `exchange-chatgpt-app-link-code` sudah ada secara lokal, tapi belum dideploy.
7. Resolver final untuk production masih belum dideploy.

## Manfaat

1. guru tidak perlu tahu `guru_id`
2. tool lebih aman karena identitas guru tidak diketik manual
3. alur AI generator soal terasa seperti produk jadi, bukan alat teknis

## Catatan

1. File ini belum dideploy.
2. Tabel dan function ini masih fondasi lokal.
3. Setelah kita siap, baru kita deploy bersamaan dengan flow penautan user yang jelas.
