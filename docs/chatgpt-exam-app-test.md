# Test Endpoint Draft AI Ujian

Function yang dipakai:

- `find-exam-ai-target`
- `save-exam-ai-draft`

## URL pencarian target

```text
https://optucpelkueqmlhwlbej.supabase.co/functions/v1/find-exam-ai-target
```

## Payload contoh pencarian target

Lihat file:

- [chatgpt-exam-target-sample-request.json](d:\004%20MA%20MIM\000%20App\login-supabase\docs\chatgpt-exam-target-sample-request.json)

## Contoh PowerShell pencarian target

```powershell
$body = Get-Content .\docs\chatgpt-exam-target-sample-request.json -Raw
Invoke-WebRequest `
  -Uri "https://optucpelkueqmlhwlbej.supabase.co/functions/v1/find-exam-ai-target" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```

## Perilaku yang diharapkan untuk pencarian target

1. Jika ketemu satu target:
   - response `200`
   - `status = "found"`
   - `target.jadwal_id` dan `target.kelas_target` terisi
2. Jika ada lebih dari satu target cocok:
   - response `200`
   - `status = "ambiguous"`
   - `candidates` berisi daftar kandidat untuk dipilih
3. Jika tidak ada yang cocok:
   - response `200`
   - `status = "not_found"`

## URL

```text
https://optucpelkueqmlhwlbej.supabase.co/functions/v1/save-exam-ai-draft
```

## URL baca draft existing

```text
https://optucpelkueqmlhwlbej.supabase.co/functions/v1/get-existing-exam-draft
```

## Payload contoh baca draft

Lihat file:

- [chatgpt-exam-existing-draft-sample-request.json](d:\004%20MA%20MIM\000%20App\login-supabase\docs\chatgpt-exam-existing-draft-sample-request.json)

## Contoh PowerShell baca draft

```powershell
$body = Get-Content .\docs\chatgpt-exam-existing-draft-sample-request.json -Raw
Invoke-WebRequest `
  -Uri "https://optucpelkueqmlhwlbej.supabase.co/functions/v1/get-existing-exam-draft" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```

## Payload contoh

Lihat file:

- [chatgpt-exam-app-sample-request.json](d:\004%20MA%20MIM\000%20App\login-supabase\docs\chatgpt-exam-app-sample-request.json)

## Contoh PowerShell

```powershell
$body = Get-Content .\docs\chatgpt-exam-app-sample-request.json -Raw
Invoke-WebRequest `
  -Uri "https://optucpelkueqmlhwlbej.supabase.co/functions/v1/save-exam-ai-draft" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```

## Perilaku yang diharapkan

1. Jika `shared_secret` benar dan payload valid:
   - function menyimpan draft ke `soal_ujian`
   - response `200`
2. Jika `jadwal_id` atau `guru_id` kosong:
   - response `400`
3. Jika `shared_secret` salah:
   - response `401`

## Catatan

Fase awal hanya mendukung:

- `pilihan-ganda`
- `benar-salah`
- `esai`
- `isi-titik`
