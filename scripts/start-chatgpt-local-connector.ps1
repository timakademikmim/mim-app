param(
  [int]$Port = 3030,
  [string]$Path = "/mcp"
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$serverDir = Join-Path $repoRoot "chatgpt-app\mcp-server"
$envFile = Join-Path $serverDir ".env"
$runtimeDir = Join-Path $repoRoot ".codex-temp\chatgpt-local-connector"
$serverOut = Join-Path $runtimeDir "mcp-http.out.log"
$serverErr = Join-Path $runtimeDir "mcp-http.err.log"
$tunnelLog = Join-Path $runtimeDir "cloudflared.log"
$serverPidFile = Join-Path $runtimeDir "mcp-http.pid"
$tunnelPidFile = Join-Path $runtimeDir "cloudflared.pid"
$cloudflared = "C:\Program Files (x86)\cloudflared\cloudflared.exe"

function Read-EnvFile([string]$path) {
  $result = @{}
  if (!(Test-Path $path)) { return $result }
  foreach ($line in Get-Content -Path $path) {
    $trimmed = [string]$line
    $trimmed = $trimmed.Trim()
    if (!$trimmed -or $trimmed.StartsWith("#")) { continue }
    $idx = $trimmed.IndexOf("=")
    if ($idx -lt 1) { continue }
    $key = $trimmed.Substring(0, $idx).Trim()
    $value = $trimmed.Substring($idx + 1).Trim()
    $result[$key] = $value
  }
  return $result
}

function Stop-ExistingProcess([string]$pidFile) {
  if (!(Test-Path $pidFile)) { return }
  try {
    $pidValue = [int](Get-Content -Path $pidFile -Raw).Trim()
    $proc = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($proc) {
      Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
    }
  } catch {
  }
  Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

function Wait-ForHttp([string]$url, [int]$timeoutSeconds = 25) {
  $deadline = (Get-Date).AddSeconds($timeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      $res = Invoke-RestMethod -Method Get -Uri $url -TimeoutSec 3
      if ($res) { return $true }
    } catch {
    }
    Start-Sleep -Milliseconds 700
  }
  return $false
}

function Wait-ForTunnelUrl([string]$logPath, [int]$timeoutSeconds = 90) {
  $deadline = (Get-Date).AddSeconds($timeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Test-Path $logPath) {
      $content = (Get-Content -Path $logPath -Raw)
      if ($content) {
        $matches = [regex]::Matches($content, "https://[a-z0-9-]+\.trycloudflare\.com")
        if ($matches.Count -gt 0) {
          return $matches[$matches.Count - 1].Value
        }
      }
    }
    Start-Sleep -Milliseconds 700
  }
  return $null
}

if (!(Test-Path $envFile)) {
  Write-Error "File .env belum ada di $serverDir. Salin dulu dari .env.example."
  exit 1
}

if (!(Test-Path $cloudflared)) {
  Write-Error "cloudflared belum ditemukan di $cloudflared"
  exit 1
}

New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null

Stop-ExistingProcess $serverPidFile
Stop-ExistingProcess $tunnelPidFile

$envMap = Read-EnvFile $envFile
$serverSecret = [string]$envMap["EXAM_AI_APP_SHARED_SECRET"]
$serverSecret = $serverSecret.Trim()
$resolverSecret = [string]$envMap["CHATGPT_APP_GURU_RESOLVER_SHARED_SECRET"]
$resolverSecret = $resolverSecret.Trim()

if (!$serverSecret) {
  Write-Error "EXAM_AI_APP_SHARED_SECRET belum terisi di $envFile"
  exit 1
}

if (!$resolverSecret) {
  $resolverSecret = $serverSecret
}

Remove-Item -LiteralPath $serverOut, $serverErr, $tunnelLog -Force -ErrorAction SilentlyContinue

$serverCmd = @(
  "set EXAM_AI_APP_SHARED_SECRET=$serverSecret",
  "set CHATGPT_APP_GURU_RESOLVER_SHARED_SECRET=$resolverSecret",
  "set MCP_HTTP_PORT=$Port",
  "set MCP_HTTP_PATH=$Path",
  "node --enable-source-maps `"$serverDir\dist\http-server.js`""
) -join " && "

$serverProc = Start-Process `
  -FilePath "cmd.exe" `
  -ArgumentList "/c $serverCmd" `
  -RedirectStandardOutput $serverOut `
  -RedirectStandardError $serverErr `
  -PassThru `
  -WindowStyle Hidden

Set-Content -Path $serverPidFile -Value $serverProc.Id

$healthUrl = "http://127.0.0.1:$Port/healthz"
if (!(Wait-ForHttp -url $healthUrl)) {
  Write-Error "MCP HTTP server gagal hidup. Cek log: $serverOut dan $serverErr"
  exit 1
}

$tunnelProc = Start-Process `
  -FilePath $cloudflared `
  -ArgumentList "tunnel --url http://127.0.0.1:$Port --logfile `"$tunnelLog`"" `
  -PassThru `
  -WindowStyle Hidden

Set-Content -Path $tunnelPidFile -Value $tunnelProc.Id

$publicUrl = Wait-ForTunnelUrl -logPath $tunnelLog
if (!$publicUrl) {
  Write-Error "Cloudflare quick tunnel tidak mengeluarkan URL. Cek log: $tunnelLog"
  exit 1
}

$connectorUrl = "$publicUrl$Path"

Write-Host ""
Write-Host "MCP server lokal aktif." -ForegroundColor Green
Write-Host "Health      : $healthUrl"
Write-Host "Public root : $publicUrl"
Write-Host "Connector   : $connectorUrl" -ForegroundColor Cyan
Write-Host ""
Write-Host "Proses berjalan:" -ForegroundColor Yellow
Write-Host "  MCP HTTP    PID $($serverProc.Id)"
Write-Host "  cloudflared PID $($tunnelProc.Id)"
Write-Host ""
Write-Host "Untuk menghentikan keduanya, jalankan:"
Write-Host "  .\scripts\stop-chatgpt-local-connector.ps1"
