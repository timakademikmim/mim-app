param(
  [int]$Port = 3030,
  [string]$Path = "/mcp"
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$serverDir = Join-Path $repoRoot "chatgpt-app\mcp-server"
$envFile = Join-Path $serverDir ".env"

if (!(Test-Path $serverDir)) {
  Write-Error "Folder MCP server tidak ditemukan: $serverDir"
  exit 1
}

if (!(Test-Path $envFile)) {
  Write-Host "File .env belum ada di $serverDir" -ForegroundColor Yellow
  Write-Host "Salin dulu dari .env.example lalu isi secret yang dibutuhkan." -ForegroundColor Yellow
  Write-Host "Contoh:" -ForegroundColor Cyan
  Write-Host "  Copy-Item chatgpt-app\mcp-server\.env.example chatgpt-app\mcp-server\.env"
  exit 1
}

$env:MCP_HTTP_PORT = [string]$Port
$env:MCP_HTTP_PATH = $Path

Write-Host "Menjalankan MCP HTTP server..." -ForegroundColor Cyan
Write-Host "Port : $Port"
Write-Host "Path : $Path"
Write-Host "Root : http://localhost:$Port/"
Write-Host "Health: http://localhost:$Port/healthz"
Write-Host "MCP  : http://localhost:$Port$Path"

Push-Location $serverDir
try {
  cmd /c npm run dev:http
} finally {
  Pop-Location
}
