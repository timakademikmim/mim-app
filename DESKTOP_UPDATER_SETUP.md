# Setup Auto Update Desktop (Tauri v2)

Dokumen ini untuk menyiapkan update otomatis aplikasi desktop lewat GitHub Release.

## 1. Generate key updater (sekali saja)

Jalankan di lokal:

```powershell
npx tauri signer generate -w ~/.tauri/mim-updater.key
```

Output akan memberi:
- `Private key` (untuk signing release update)
- `Public key` (dipakai app untuk verifikasi update)

Simpan private key dengan aman. Jangan commit ke repo.

## 2. Set GitHub Secrets

Di repo GitHub: `Settings -> Secrets and variables -> Actions`, tambah:

- `TAURI_SIGNING_PRIVATE_KEY` = isi private key updater
- `TAURI_SIGNING_PRIVATE_KEY_PASSWORD` = password key updater
- `TAURI_UPDATER_PUBKEY` = public key updater

## 3. Konfigurasi yang sudah dipasang

- Updater endpoint:
  - `https://github.com/timakademikmim/mim-app/releases/latest/download/latest.json`
- Auto-check update saat app desktop startup (mode release).
- Auto-download + install update jika ada versi baru.
- Build Tauri menghasilkan updater artifacts (`latest.json`, `.sig`, dll).

File terkait:
- `src-tauri/tauri.conf.json`
- `src-tauri/src/main.rs`
- `.github/workflows/desktop-release.yml`

## 4. Cara rilis versi baru

Naikkan versi di:
- `src-tauri/tauri.conf.json` -> field `version`

Lalu commit + push, kemudian buat tag:

```powershell
git add .
git commit -m "release: bump desktop version to x.y.z"
git tag vx.y.z
git push origin main --tags
```

Workflow `Desktop Release` akan:
- build app desktop,
- sign updater artifacts,
- upload ke GitHub Release tag tersebut.

## 5. Cara user menerima update

User cukup buka app desktop seperti biasa.
Saat startup, app cek update baru. Jika ada, app akan download+install otomatis.

## 6. Catatan penting

- Untuk test updater, wajib install build release (bukan `tauri dev`).
- Versi baru harus lebih tinggi dari versi yang terpasang user.
- Jika release gagal, cek tab `Actions` di GitHub untuk detail error.
