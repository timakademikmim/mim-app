param(
  [int]$Port = 3030
)

$cloudflared = "C:\Program Files (x86)\cloudflared\cloudflared.exe"

if (!(Test-Path $cloudflared)) {
  Write-Error "cloudflared belum ditemukan di $cloudflared"
  exit 1
}

Write-Host "Menjalankan Cloudflare quick tunnel..." -ForegroundColor Cyan
Write-Host "Local target : http://127.0.0.1:$Port"
Write-Host "Catatan      : jendela ini harus tetap terbuka selama ChatGPT memakai tunnel."

& $cloudflared tunnel --url "http://127.0.0.1:$Port"
