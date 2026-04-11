param(
  [string]$BaseUrl = "http://127.0.0.1:3030/mcp"
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$serverDir = Join-Path $repoRoot "chatgpt-app\mcp-server"

if (!(Test-Path $serverDir)) {
  Write-Error "Folder MCP server tidak ditemukan: $serverDir"
  exit 1
}

$env:MCP_HTTP_BASE_URL = $BaseUrl

Write-Host "Menjalankan smoke test MCP HTTP ke $BaseUrl" -ForegroundColor Cyan

Push-Location $serverDir
try {
  cmd /c npm run smoke:http
} finally {
  Pop-Location
}
