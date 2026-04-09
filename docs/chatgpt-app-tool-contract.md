# Tool Contract ChatGPT App

Dokumen ini mendefinisikan kontrak minimal untuk ChatGPT App yang terhubung ke generator soal ujian.

## Tujuan

ChatGPT App mengumpulkan kebutuhan guru lalu menyimpan hasilnya sebagai draft ke sistem sekolah lewat function:

- `save-exam-ai-draft`

## Tool Minimal

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

1. `jadwal_id` ada
2. `guru_id` ada
3. `questions` minimal 1
4. nomor soal berurutan
5. untuk `pilihan-ganda`, opsi minimal 2

## Langkah Berikutnya

Setelah tool minimal ini stabil, tool tambahan yang masuk akal:

1. `list_exam_targets`
2. `get_existing_exam_draft`
3. `revise_exam_draft`
