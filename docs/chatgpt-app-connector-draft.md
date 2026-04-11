# Draft Connector ChatGPT Untuk Generator Soal

Dokumen ini menyiapkan isi awal saat kita membuat connector MCP di ChatGPT.

## Nama Connector

`MIM Generator Soal`

## Deskripsi Singkat

Connector ini membantu guru:

1. menautkan akun ChatGPT ke identitas guru di sistem
2. mencari target ujian berdasarkan kelas, mapel, dan tanggal
3. membaca draft soal yang sudah ada
4. membuat atau merevisi draft soal langsung ke editor ujian

## URL MCP

Gunakan endpoint MCP yang sudah aktif dan bisa diakses lewat HTTPS.

Format:

```text
https://domain-kamu/mcp
```

Saat testing lokal:

```text
https://xxxx.ngrok-free.app/mcp
```

## Alur Yang Diharapkan Dari ChatGPT

1. Saat user belum tertaut:
   - cek dengan `resolve_chatgpt_app_guru`
   - jika `not_linked`, minta kode tautan 24 jam
   - tukar dengan `exchange_chatgpt_app_link_code`
2. Saat user sudah tertaut:
   - cari target dengan `find_exam_ai_target`
3. Jika target ketemu:
   - cek draft lama dengan `get_existing_exam_draft`
4. Jika user ingin membuat baru:
   - gunakan `save_exam_ai_draft`
5. Jika user ingin memperbarui draft lama:
   - gunakan `revise_exam_ai_draft`

## Tool Yang Akan Terlihat

1. `resolve_chatgpt_app_guru`
2. `find_exam_ai_target`
3. `get_existing_exam_draft`
4. `exchange_chatgpt_app_link_code`
5. `save_exam_ai_draft`
6. `revise_exam_ai_draft`

## Catatan Produk

1. Draft yang dibuat AI tetap harus direview guru sebelum `Kirim Soal`.
2. Tool ini hanya menyentuh draft editor ujian, bukan mengirim soal final otomatis.
3. Jalur linking memakai kode 24 jam dari admin agar guru tidak perlu mengurus `guru_id`.
