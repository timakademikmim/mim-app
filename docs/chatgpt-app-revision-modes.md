# Mode Revisi Draft AI

Dokumen ini menjelaskan mode revisi draft yang sudah disiapkan di MCP server lokal.

## Tool

`revise_exam_ai_draft`

## Mode Yang Didukung

### 1. `replace_all`

Gunanya:

- mengganti penuh isi draft lama dengan draft baru

Cocok saat:

1. guru ingin reset soal
2. materi berganti
3. draft lama dianggap kurang bagus

### 2. `append_questions`

Gunanya:

- menambahkan soal baru ke draft lama

Cocok saat:

1. guru ingin menambah beberapa nomor
2. draft lama sudah bagus dan hanya perlu dilengkapi

## Perilaku Aman

1. tool selalu membaca draft lama dulu
2. tool tidak men-submit final
3. hasil revisi tetap masuk sebagai `draft`
4. ringkasan respons menyebut:
   - mode revisi
   - jumlah soal lama
   - jumlah soal baru

## Catatan

1. Mode ini masih lokal di MCP server.
2. Belum dibuat sebagai Supabase function terpisah.
3. Ini sengaja supaya logika revisi bisa kita uji dulu tanpa deploy tambahan.
