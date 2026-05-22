# Trivy Security Scan für alle eigenen Docker-Images
# Aufruf:   .\scripts\security-scan.ps1
# Ergebnis: docs/security-scans/<image>.txt + Summary in Konsole

$ErrorActionPreference = "Stop"

$images = @(
    "legocitytimes/content-service:latest",
    "legocitytimes/search-service:latest",
    "legocitytimes/frontend:latest"
)

$outDir = Join-Path (Join-Path (Join-Path $PSScriptRoot "..") "docs") "security-scans"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$outDirAbs = (Resolve-Path $outDir).Path

Write-Host "==> Trivy Scan startet ($(Get-Date -Format 'yyyy-MM-dd HH:mm'))" -ForegroundColor Cyan
Write-Host "    Output: $outDirAbs"
Write-Host ""

$summary = @()

foreach ($img in $images) {
    $safeName = $img.Replace("/", "_").Replace(":", "_")
    $txtPath  = "/out/$safeName.txt"
    $jsonPath = "/out/$safeName.json"

    Write-Host "==> Scanne $img" -ForegroundColor Yellow

    # Tabellen-Report (lesbar)
    docker run --rm `
        -v /var/run/docker.sock:/var/run/docker.sock `
        -v "${outDirAbs}:/out" `
        aquasec/trivy:latest image `
        --severity LOW,MEDIUM,HIGH,CRITICAL `
        --format table `
        --output $txtPath `
        $img

    # JSON-Report (für Auswertung)
    docker run --rm `
        -v /var/run/docker.sock:/var/run/docker.sock `
        -v "${outDirAbs}:/out" `
        aquasec/trivy:latest image `
        --severity LOW,MEDIUM,HIGH,CRITICAL `
        --format json `
        --output $jsonPath `
        $img

    # Findings zählen
    $jsonFile = Join-Path $outDirAbs "$safeName.json"
    if (Test-Path $jsonFile) {
        $data = Get-Content $jsonFile -Raw | ConvertFrom-Json
        $crit = 0; $high = 0; $med = 0; $low = 0
        foreach ($result in $data.Results) {
            if ($result.Vulnerabilities) {
                foreach ($v in $result.Vulnerabilities) {
                    switch ($v.Severity) {
                        "CRITICAL" { $crit++ }
                        "HIGH"     { $high++ }
                        "MEDIUM"   { $med++ }
                        "LOW"      { $low++ }
                    }
                }
            }
        }
        $summary += [PSCustomObject]@{
            Image    = $img
            Critical = $crit
            High     = $high
            Medium   = $med
            Low      = $low
        }
    }
}

Write-Host ""
Write-Host "==> Zusammenfassung" -ForegroundColor Cyan
$summary | Format-Table -AutoSize

$totalCrit = ($summary | Measure-Object Critical -Sum).Sum
if ($totalCrit -gt 0) {
    Write-Host "WARNUNG: $totalCrit kritische Schwachstellen gefunden!" -ForegroundColor Red
    exit 1
} else {
    Write-Host "OK: Keine kritischen Schwachstellen." -ForegroundColor Green
}
