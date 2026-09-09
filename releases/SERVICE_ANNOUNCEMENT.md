# Pengumuman Layanan Android

Setelah APK yang memuat fitur ini dipasang, pengumuman dapat diubah tanpa
membuat APK atau menaikkan versi. Ubah `serviceNotice` pada
`releases/mim-app-update.json`, lalu commit dan push ke branch `main`.

Contoh pengumuman penghentian layanan:

```json
"serviceNotice": {
  "enabled": true,
  "id": "penghentian-layanan-2026-10-15",
  "title": "Informasi Penghentian Layanan",
  "message": "Layanan MIM App akan dihentikan pada 15 Oktober 2026. Silakan selesaikan kebutuhan administrasi sebelum tanggal tersebut.",
  "acknowledgementLabel": "Saya mengerti",
  "startsAt": "2026-10-01T00:00:00+08:00",
  "endsAt": "",
  "blockAt": "2026-10-15T00:00:00+08:00"
}
```

- `enabled`: aktifkan atau matikan pengumuman.
- `id`: wajib unik untuk setiap isi pengumuman baru. Pengguna yang sudah
  menekan tombol konfirmasi tidak akan melihat pengumuman dengan ID yang sama.
- `startsAt`: waktu mulai tampil. Kosong berarti langsung tampil.
- `endsAt`: waktu berhenti tampil untuk pengumuman biasa. Kosong berarti tetap
  tampil untuk pengguna yang belum mengonfirmasi.
- `blockAt`: waktu aplikasi ditutup dari akses. Kosongkan bila hanya ingin
  memberi informasi.

Gunakan format waktu ISO dengan zona Makassar `+08:00`. Perangkat yang sedang
offline tidak dapat mengambil pesan baru; untuk penghentian layanan, tetap
gunakan pengumuman WhatsApp atau kanal resmi lain sebagai pendamping.
