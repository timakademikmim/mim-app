# MIM App Android

Scaffold awal aplikasi Android native untuk panel guru, dibuat terpisah dari web app agar maintenance lebih mudah.

## Yang sudah ada

- Proyek Android Studio baru di folder `mimappandroid`
- UI Jetpack Compose
- Alur `Welcome/Splash -> Login -> Dashboard Guru`
- Session persisten dengan `DataStore`
- Cache dashboard lokal ke internal storage (`filesDir`) dalam bentuk JSON
- Refresh server simulatif untuk menggambarkan pola `offline-first`
- Preview UI yang bisa dilihat langsung di Android Studio

## Cara buka di Android Studio

1. Pilih `Open`.
2. Arahkan ke folder:
   - `login-supabase/mimappandroid`
3. Tunggu Gradle Sync selesai.
4. Jalankan `app` ke emulator atau buka file Compose lalu lihat `Preview`.

## File penting

- `app/src/main/java/com/mim/guruapp/MainActivity.kt`
- `app/src/main/java/com/mim/guruapp/GuruAppViewModel.kt`
- `app/src/main/java/com/mim/guruapp/data/storage/SessionStore.kt`
- `app/src/main/java/com/mim/guruapp/data/storage/GuruCacheStore.kt`
- `app/src/main/java/com/mim/guruapp/ui/screens/*`

## Catatan tahap ini

Ini masih scaffold UI + fondasi data lokal. Sinkronisasi ke Supabase, Room, dan WorkManager belum disambungkan penuh. Target tahap berikutnya:

1. Ganti cache JSON menjadi Room
2. Tambah antrean input offline untuk absensi/nilai/soal
3. Sinkron background ke Supabase
4. Pecah dashboard menjadi fitur mapel, detail mapel, patron materi, dan soal mapel
