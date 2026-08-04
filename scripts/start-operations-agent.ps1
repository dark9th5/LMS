$ErrorActionPreference = "Stop"
Set-Location (Resolve-Path "$PSScriptRoot\..")
$runtime = Join-Path (Get-Location) ".runtime"
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
$pidFile = Join-Path $runtime "operations-agent.pid"
$logFile = Join-Path $runtime "operations-agent.log"
$errorFile = Join-Path $runtime "operations-agent.error.log"

if (Test-Path $pidFile) {
  $existingPid = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
  if ($existingPid -and (Get-Process -Id $existingPid -ErrorAction SilentlyContinue)) {
    Write-Host "Operations agent đang chạy (PID $existingPid)."
    exit 0
  }
  Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

$python = Get-Command python3 -ErrorAction SilentlyContinue
if (-not $python) { $python = Get-Command python -ErrorAction SilentlyContinue }
if (-not $python) { throw "Không tìm thấy Python 3. Hãy cài Python 3 hoặc chạy operations-agent.py thủ công." }
if (-not (Test-Path .env)) { throw "Thiếu .env. Hãy chạy setup trước." }

$process = Start-Process -FilePath $python.Source `
  -ArgumentList @("$PSScriptRoot\operations-agent.py", "--interval", "10") `
  -WorkingDirectory (Get-Location) `
  -RedirectStandardOutput $logFile `
  -RedirectStandardError $errorFile `
  -WindowStyle Hidden `
  -PassThru
Set-Content -Path $pidFile -Value $process.Id -Encoding ascii
Start-Sleep -Seconds 1
if (-not (Get-Process -Id $process.Id -ErrorAction SilentlyContinue)) {
  throw "Operations agent không khởi động được. Xem $errorFile"
}
Write-Host "Operations agent đã chạy (PID $($process.Id))."
