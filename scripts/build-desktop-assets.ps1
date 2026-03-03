$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$outDir = Join-Path $root "dist-tauri-web"

if (Test-Path $outDir) {
  Remove-Item $outDir -Recurse -Force
}
New-Item -Path $outDir -ItemType Directory | Out-Null

$rootFiles = @(
  "*.html",
  "*.js",
  "*.css",
  "*.ttf",
  "*.png"
)

foreach ($pattern in $rootFiles) {
  Get-ChildItem -Path $root -Filter $pattern -File | ForEach-Object {
    Copy-Item -Path $_.FullName -Destination $outDir -Force
  }
}

$pagesSrc = Join-Path $root "pages"
$pagesDst = Join-Path $outDir "pages"
if (Test-Path $pagesSrc) {
  Copy-Item -Path $pagesSrc -Destination $pagesDst -Recurse -Force
}

Write-Host "Desktop assets built at: $outDir"
