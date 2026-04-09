# Tool Contract ChatGPT App

Dokumen ini mendefinisikan kontrak minimal untuk ChatGPT App yang terhubung ke generator soal ujian.

## Tujuan

ChatGPT App mengumpulkan kebutuhan guru lalu menyimpan hasilnya sebagai draft ke sistem sekolah lewat function:

- `save-exam-ai-draft`

## Tool Minimal

### `find_exam_ai_target`

Fungsi:

- mencari target ujian yang tepat dari `guru + kelas + mapel + tanggal`
- mengembalikan `jadwal_id` dan `kelas_target` untuk dipakai tool simpan draft

Input:

```json
{
  "guru_id": "uuid-guru",
  "kelas": "X",
  "mapel": "Sharf",
  "tanggal": "2026-04-10",
  "nama_ujian": "UTS Semester Genap Tahun Ajaran 2025/2026",
  "jenis": "UTS"
}
```

Output jika ketemu satu target:

```json
{
  "ok": true,
  "status": "found",
  "target": {
    "jadwal_id": "uuid-jadwal",
    "kelas_target": "X",
    "guru_id": "uuid-guru",
    "mapel_label": "Sharf",
    "nama_ujian": "UTS Semester Genap Tahun Ajaran 2025/2026",
    "jenis_ujian": "UTS",
    "tanggal": "2026-04-10",
    "jam_mulai": "09:00:00",
    "jam_selesai": "10:00:00"
  }
}
```

Output jika ada lebih dari satu kandidat:

```json
{
  "ok": true,
  "status": "ambiguous",
  "target": null,
  "candidates": [
    {
      "jadwal_id": "uuid-jadwal-1",
      "kelas_target": "X A"
    },
    {
      "jadwal_id": "uuid-jadwal-2",
      "kelas_target": "X B"
    }
  ]
}
```

### `save_exam_ai_draft`

Fungsi:

- menyimpan draft soal hasil AI ke `soal_ujian`

Input:

```json
{
  "jadwal_id": "uuid-jadwal",
  "kelas_target": "IX B",
  "guru_id": "uuid-guru",
  "guru_nama": "Nama Guru",
  "instruksi": "Kerjakan soal berikut dengan benar.",
  "instruksi_lang": "ID",
  "source": "chatgpt-app",
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
  "questions": [
    {
      "no": 1,
      "type": "pilihan-ganda",
      "text": "Teks soal",
      "options": {
        "a": "Pilihan A",
        "b": "Pilihan B",
        "c": "Pilihan C",
        "d": "Pilihan D"
      },
      "answer": "A"
    }
  ]
}
```

Output sukses:

```json
{
  "ok": true,
  "draft": {
    "id": "uuid-row-soal",
    "jadwal_id": "uuid-jadwal",
    "kelas_target": "IX B",
    "guru_id": "uuid-guru",
    "status": "draft"
  },
  "summary": {
    "jadwal_id": "uuid-jadwal",
    "kelas_target": "IX B",
    "guru_id": "uuid-guru",
    "question_count": 10,
    "section_count": 1
  }
}
```

## Tipe Soal Fase 1

Didukung:

- `pilihan-ganda`
- `benar-salah`
- `esai`
- `isi-titik`

Belum didukung:

- `cari-kata`
- `teka-silang`
- `pasangkan-kata`

## Perilaku Yang Diinginkan Dari ChatGPT

ChatGPT App sebaiknya:

1. memastikan jumlah soal cocok dengan permintaan guru
2. tidak langsung submit final
3. selalu menyimpan sebagai draft
4. menghasilkan struktur JSON yang bersih
5. menghindari field tambahan yang tidak dibaca editor

## Validasi Sebelum Memanggil Tool

Sebelum memanggil tool, ChatGPT App harus memastikan:

1. panggil `find_exam_ai_target` lebih dulu jika `jadwal_id` belum diketahui
2. `jadwal_id` ada
3. `guru_id` ada
4. `questions` minimal 1
5. nomor soal berurutan
6. untuk `pilihan-ganda`, opsi minimal 2

## Langkah Berikutnya

Setelah tool minimal ini stabil, tool tambahan yang masuk akal:

1. `get_existing_exam_draft`
2. `revise_exam_draft`

### `get_existing_exam_draft`

Fungsi:

- membaca draft ujian yang sudah tersimpan untuk `jadwal_id + guru_id + kelas_target`
- berguna untuk mengecek apakah ChatGPT App perlu membuat draft baru atau merevisi draft yang ada

Input:

```json
{
  "jadwal_id": "uuid-jadwal",
  "guru_id": "uuid-guru",
  "kelas_target": "X"
}
```

Output jika draft ada:

```json
{
  "ok": true,
  "status": "found",
  "draft": {
    "id": "uuid-row-soal",
    "status": "draft",
    "parsed_questions_json": {
      "questions": [],
      "sections": [],
      "meta": {
        "source": "chatgpt-app"
      }
    }
  }
}
```
