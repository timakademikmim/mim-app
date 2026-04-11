# Checklist Kesiapan ChatGPT App Generator Soal

Status per 9 April 2026.

## Sudah Siap

1. Draft AI bisa disimpan ke `soal_ujian`
2. Editor guru bisa membaca draft AI
3. Target ujian bisa dicari dari:
   - guru
   - kelas
   - mapel
   - tanggal
4. Draft lama bisa dibaca
5. Draft lama bisa direvisi di MCP lokal
6. Linking user ChatGPT ke guru sudah live:
   - resolver live
   - exchange link code live
7. MCP server lokal sudah bisa smoke test ke backend live
8. MCP server sudah punya mode `streamable HTTP` lokal pada endpoint `/mcp`

## Belum Siap Untuk Dipakai Guru Nyata

1. MCP server belum ditempatkan di endpoint HTTPS yang stabil untuk ChatGPT App produksi
2. Belum ada konfigurasi final ChatGPT App yang benar-benar terhubung ke OpenAI ChatGPT
3. Belum ada tes end-to-end dari ChatGPT nyata ke editor guru
4. Belum ada guardrail produksi untuk:
   - konflik revisi
   - audit log revisi
   - pembatasan akses per guru

## Siap Untuk Prototype Internal

Jika targetnya hanya uji internal pengembang, kita sudah dekat.

Yang masih perlu:

1. hidupkan resolver guru live
2. pilih bentuk deployment MCP/Apps SDK
3. tes satu flow end-to-end

## Siap Untuk Pilot Guru

Agar aman dipakai 1-2 guru sebagai pilot, saya ingin minimal ini selesai:

1. linking akun ChatGPT ke guru live
2. revisi draft diuji untuk `replace_all` dan `append_questions`
3. ada alur fallback jika target ujian ambigu
4. ada log ringkas aktivitas draft AI
5. ada panduan singkat penggunaan untuk guru

## Estimasi Realistis

Jika kita lanjut tanpa ganti arah besar:

1. Prototype internal:
   - sekitar 1 sampai 2 sesi kerja lagi
2. Pilot terbatas:
   - sekitar 2 sampai 4 hari kerja
3. Siap dipakai lebih luas:
   - sekitar 5 sampai 7 hari kerja

Estimasi ini mengasumsikan:

1. kita tidak mengubah arsitektur lagi
2. tidak ada blocker baru dari integrasi ChatGPT App
3. deploy dan auth berjalan normal

## Penilaian Jujur

Kalau ditanya hari ini apakah “sudah bisa dipakai guru langsung di ChatGPT?”, jawabannya:

- belum

Kalau ditanya apakah fondasi intinya sudah terbukti dan arah kita benar, jawabannya:

- ya

Menurut saya progresnya sekarang sudah masuk kisaran:

- 70% sampai 80% untuk prototype
- 50% sampai 60% untuk siap dipakai guru nyata
