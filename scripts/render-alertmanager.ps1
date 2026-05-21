Param()
$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot '..\.env'
$template = Join-Path $PSScriptRoot '..\monitoring\alertmanager\alertmanager.yml.tmpl'
$out = Join-Path $PSScriptRoot '..\monitoring\alertmanager\alertmanager.generated.yml'
if (-not (Test-Path $envFile)) { Write-Error "Missing .env file at $envFile"; exit 1 }
$lines = Get-Content $envFile | Where-Object { $_ -match '=' }
$map = @{}
foreach ($l in $lines) {
    $parts = $l -split '=',2
    $map[$parts[0].Trim()] = $parts[1].Trim()
}
if (-not $map.ContainsKey('SLACK_API_URL')) { Write-Error "SLACK_API_URL not set in .env"; exit 1 }
$tpl = Get-Content $template -Raw
$tpl = $tpl.Replace('${SLACK_API_URL}', $map['SLACK_API_URL'])
Set-Content -Path $out -Value $tpl -NoNewline
Write-Host "Rendered $out"