$ErrorActionPreference = "Stop"
Set-Location (Resolve-Path "$PSScriptRoot\..")
$pidFile = Join-Path (Get-Location) ".runtime\operations-agent.pid"
if (-not (Test-Path $pidFile)) { Write-Host "Không có operations agent đang được quản lý."; exit 0 }
$agentPid = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
if ($agentPid) { Stop-Process -Id $agentPid -ErrorAction SilentlyContinue }
Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
Write-Host "Operations agent đã dừng."
