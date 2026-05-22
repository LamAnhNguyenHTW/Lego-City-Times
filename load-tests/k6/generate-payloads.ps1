param(
  [int]$SizeMB = 5,
  [string]$OutputDir = $(Join-Path $PSScriptRoot 'payloads')
)

$ErrorActionPreference = 'Stop'

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$bytes = $SizeMB * 1024 * 1024

$payload = [ordered]@{
  id          = 'loadtest-article'
  title       = 'Load Test Article'
  subtitle    = 'k6 payload'
  content     = [string]::new([char]'a', $bytes)
  author      = 'LoadTest'
  slug        = 'loadtest-article'
  status      = 'PUBLISHED'
  publishedAt = '2026-05-21T00:00:00Z'
}

$json = $payload | ConvertTo-Json -Compress

$outFile = Join-Path $OutputDir 'article-index-5mb.json'
[System.IO.File]::WriteAllText($outFile, $json, [System.Text.Encoding]::UTF8)

$size = (Get-Item $outFile).Length
Write-Host "Wrote $outFile ($size bytes)"
