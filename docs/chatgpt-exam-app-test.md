# Test Endpoint Draft AI Ujian

Function yang dipakai:

- `save-exam-ai-draft`

## URL

```text
https://optucpelkueqmlhwlbej.supabase.co/functions/v1/save-exam-ai-draft
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
