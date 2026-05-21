Param(
    [switch]$Build
)
$ErrorActionPreference = 'Stop'
& "$PSScriptRoot\render-alertmanager.ps1"
$buildArg = ''
if ($Build) { $buildArg = '--build' }
& docker compose up -d $buildArg