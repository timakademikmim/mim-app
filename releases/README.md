# MIM App Android Release

Folder ini menyimpan manifest update aplikasi native Android.

## Alur rilis manual

1. Naikkan `versionCode` dan `versionName` di `mimappandroid/app/build.gradle.kts`.
2. Build APK, lalu upload file `MIM APP.apk` ke GitHub Releases.
3. Update `releases/mim-app-update.json`:
   - `versionCode` harus lebih besar dari versi yang sudah terpasang di HP.
   - `versionName` mengikuti label rilis.
   - `apkUrl` mengarah ke file APK di GitHub Releases.
   - `mandatory` isi `true` jika user wajib update.
4. Push perubahan manifest ke branch `main`.

App membaca manifest dari:

`https://raw.githubusercontent.com/timakademikmim/mim-app/main/releases/mim-app-update.json`

Catatan Android:

Install APK tidak bisa silent install. Setelah download selesai, aplikasi akan membuka installer Android dan user tetap perlu menekan tombol install.

## Alur rilis otomatis GitHub Actions

Workflow native Android ada di `.github/workflows/native-android-release.yml`.

Cara rilis:

1. Pastikan GitHub repo memiliki secrets:
   - `MIM_ANDROID_KEYSTORE_BASE64`
   - `MIM_ANDROID_KEYSTORE_PASSWORD`
   - `MIM_ANDROID_KEY_ALIAS`
   - `MIM_ANDROID_KEY_PASSWORD`
2. Buat tag rilis, misalnya `native-android-v0.2.0`, lalu push tag itu ke GitHub.
3. Workflow akan membuat signed APK bernama `MIM APP.apk`.
4. Workflow akan upload APK ke GitHub Releases.
5. Workflow akan update `releases/mim-app-update.json` di branch `main`.

Penting:

APK update hanya bisa mengganti APK lama jika signature-nya sama. Jadi APK pertama yang dipasang user sebaiknya berasal dari build signed release workflow ini, bukan debug APK lokal.
