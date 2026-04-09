# ChatGPT App Generator Soal

Dokumen ini merangkum arsitektur yang paling masuk akal untuk generator soal berbasis ChatGPT tanpa membebankan API ke sistem web sekolah.

## Tujuan

Guru cukup menulis:

- materi
- jumlah soal
- model soal
- bahasa
- tingkat kesulitan

Lalu hasil AI masuk ke draft editor soal ujian tanpa copy manual satu per satu.

## Prinsip Arsitektur

1. AI berjalan di ChatGPT App.
2. Sistem sekolah tetap menjadi sumber data jadwal ujian dan penyimpanan draft final.
3. ChatGPT App tidak menulis langsung ke textarea browser.
4. ChatGPT App mengirim payload draft ke backend kita.
5. Editor ujian membaca draft itu dari `soal_ujian.questions_json`.

## Alur Pengguna

1. Guru membuka ChatGPT.
2. Guru memilih app generator soal.
3. Guru memilih jadwal ujian dan kelas target.
4. Guru mengisi materi dan aturan pembuatan soal.
5. ChatGPT menghasilkan draft terstruktur.
6. Tool backend `save-exam-ai-draft` menyimpan draft ke `soal_ujian`.
7. Guru membuka editor ujian di web sekolah.
8. Draft AI sudah muncul dan bisa diedit sebelum dikirim final.

## Struktur Draft Yang Sudah Cocok Dengan Editor

Editor saat ini menyimpan payload di `soal_ujian.questions_json` dengan bentuk:

```json
{
  "questions": [],
  "sections": []
}
```

Fase awal yang disarankan:

- `pilihan-ganda`
- `benar-salah`
- `esai`
- `isi-titik`

Belum dulu:

- `cari-kata`
- `teka-silang`
- `pasangkan-kata`

karena model itu butuh generator khusus, bukan hanya teks.

## Kontrak Payload Fase 1

Request ke backend:

```json
{
  "shared_secret": "secret-app",
  "jadwal_id": "uuid-jadwal",
  "kelas_target": "IX B",
  "guru_id": "uuid-guru-atau-id-karyawan",
  "guru_nama": "Nama Guru",
  "instruksi": "Kerjakan soal berikut dengan benar.",
  "instruksi_lang": "ID",
  "questions": [
    {
      "no": 1,
      "type": "pilihan-ganda",
      "text": "Apa arti kata kitab?",
      "options": {
        "a": "Buku",
        "b": "Rumah",
        "c": "Meja",
        "d": "Pintu"
      },
      "answer": "A"
    }
  ],
  "sections": [
    {
      "type": "pilihan-ganda",
      "start": 1,
      "end": 10,
      "count": 10,
      "instruction": "",
      "score": null
    }
  ],
  "source": "chatgpt-app"
}
```

## Normalisasi Yang Dilakukan Backend

Backend akan:

1. mengurutkan soal berdasarkan `no`
2. membatasi tipe soal hanya yang didukung
3. membentuk `questions_json`
4. menyimpan sebagai `status = draft`
5. update row lama jika kombinasi `jadwal_id + guru_id + kelas_target` sudah ada

## Tool Yang Dibutuhkan Di ChatGPT App

Fase minimal:

1. `save_exam_ai_draft`
   - menyimpan draft ke sistem sekolah

Fase berikutnya:

1. `list_exam_targets`
   - mengambil daftar jadwal/kelas yang bisa dipilih guru
2. `get_existing_exam_draft`
   - mengambil draft lama untuk direvisi AI
3. `append_exam_section`
   - menambah section baru tanpa menimpa semua draft

## Auth Yang Disarankan

Karena ChatGPT App perlu menulis ke sistem sekolah, backend perlu mekanisme identitas.

Fase paling sederhana:

1. ChatGPT App memegang `shared secret`
2. payload juga membawa `guru_id`

Fase produksi yang lebih aman:

1. guru login ke sistem sekolah dari ChatGPT App
2. backend menerbitkan token session singkat
3. tool memakai token itu saat memanggil backend

## Perubahan UI Web Yang Disarankan

Fase awal cukup kecil:

1. tampilkan label `Draft dibuat dengan AI`
2. tampilkan waktu update terakhir
3. jangan auto-submit
4. guru tetap review lalu klik `Kirim Soal`

## Urutan Pembangunan

1. selesai-kan kontrak payload fase 1
2. deploy `save-exam-ai-draft`
3. uji simpan draft manual lewat endpoint
4. hubungkan endpoint ke ChatGPT App
5. tambahkan label AI di editor web
6. baru tambah jenis soal lain

## Catatan Risiko

1. ChatGPT App tidak boleh langsung submit soal final tanpa review guru.
2. Draft harus tetap bisa diedit manual dari web.
3. Perubahan schema editor harus dijaga stabil agar tool AI tidak cepat rusak.
