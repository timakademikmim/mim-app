$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeDir = Join-Path $repoRoot ".codex-temp\chatgpt-local-connector"
$serverPidFile = Join-Path $runtimeDir "mcp-http.pid"
$tunnelPidFile = Join-Path $runtimeDir "cloudflared.pid"

function Stop-ByPidFile([string]$pidFile, [string]$label) {
  if (!(Test-Path $pidFile)) {
    Write-Host "$label: tidak ada pid file"
    return
  }

  try {
    $pidValue = [int](Get-Content -Path $pidFile -Raw).Trim()
    $proc = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($proc) {
      Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
      Write-Host "$label: proses $pidValue dihentikan"
    } else {
      Write-Host "$label: proses sudah tidak aktif"
    }
  } catch {
    Write-Host "$label: gagal membaca pid"
  }

  Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

Stop-ByPidFile $serverPidFile "MCP HTTP"
Stop-ByPidFile $tunnelPidFile "Cloudflared"
