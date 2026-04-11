# Simulasi Percakapan ChatGPT App Generator Soal

Dokumen ini menggambarkan flow ideal saat nanti ChatGPT App sudah tersambung penuh.

## Skenario 1: Draft Baru

Guru:

```text
Buatkan 10 soal pilihan ganda materi Sharf untuk kelas X, ujian tanggal 10 April 2026.
```

Alur tool:

1. `resolve_chatgpt_app_guru`
2. `find_exam_ai_target`
3. `get_existing_exam_draft`
4. `save_exam_ai_draft`

Respons yang diharapkan:

```text
Target ujian ditemukan untuk Sharf kelas X tanggal 10 April 2026.
Belum ada draft lama, jadi saya buat draft baru.

Draft soal berhasil disimpan ke editor ujian.

Ringkasan:
- Mapel: Sharf
- Kelas: X
- Tanggal: 10 April 2026
- Jumlah soal: 10
- Jenis: Pilihan Ganda

Silakan review isi soal di editor sekolah sebelum menekan Kirim Soal.
```

## Skenario 2: Draft Sudah Ada, Ganti Penuh

Guru:

```text
Draft Sharf yang kemarin kurang bagus. Ganti semua jadi 5 soal esai.
```

Alur tool:

1. `resolve_chatgpt_app_guru`
2. `find_exam_ai_target`
3. `get_existing_exam_draft`
4. ChatGPT meminta konfirmasi mode revisi `replace_all`
5. `revise_exam_ai_draft`

Respons yang diharapkan:

```text
Saya menemukan draft lama untuk ujian ini.
Karena Anda ingin mengganti semua soal, saya pakai mode revisi replace_all.

Draft soal berhasil diperbarui.
Silakan review isi soal di editor sekolah sebelum menekan Kirim Soal.
```

## Skenario 3: Draft Sudah Ada, Tambah Soal

Guru:

```text
Tambahkan 3 soal pilihan ganda lagi ke draft Sharf yang sudah ada.
```

Alur tool:

1. `resolve_chatgpt_app_guru`
2. `find_exam_ai_target`
3. `get_existing_exam_draft`
4. ChatGPT memberi tahu bahwa mode yang dipakai adalah `append_questions`
5. `revise_exam_ai_draft`

Respons yang diharapkan:

```text
Saya menemukan draft lama dan akan menambahkan soal baru dengan mode append_questions.

Draft soal berhasil diperbarui.
Nomor soal baru ditambahkan setelah nomor yang sudah ada.
Silakan review isi soal di editor sekolah sebelum menekan Kirim Soal.
```

## Skenario 4: Target Ujian Ambigu

Guru:

```text
Buatkan soal Bahasa Arab untuk kelas X tanggal 10 April 2026.
```

Alur tool:

1. `resolve_chatgpt_app_guru`
2. `find_exam_ai_target`
3. hasil `ambiguous`

Respons yang diharapkan:

```text
Saya menemukan lebih dari satu target ujian yang cocok.
Pilih salah satu target berikut agar draft tidak masuk ke kelas yang salah:
- X A
- X B
```
