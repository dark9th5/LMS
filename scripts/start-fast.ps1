$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $Root

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw "Docker chưa được cài hoặc chưa có trong PATH."
}
docker info *> $null
if ($LASTEXITCODE -ne 0) { throw "Docker Engine chưa chạy." }
if (-not (Test-Path ".env")) {
  throw "Thiếu .env. Hãy sao chép .env.example thành .env và điền các secret bắt buộc."
}

if (-not $env:COMPOSE_PARALLEL_LIMIT) { $env:COMPOSE_PARALLEL_LIMIT = "8" }
$timeout = if ($env:LMSPILOT_STARTUP_TIMEOUT) { $env:LMSPILOT_STARTUP_TIMEOUT } else { "360" }

docker compose build --pull api-gateway web
if ($LASTEXITCODE -ne 0) { throw "Build image thất bại." }
docker compose up -d --no-build --remove-orphans --wait --wait-timeout $timeout
if ($LASTEXITCODE -ne 0) { throw "Một hoặc nhiều service không đạt trạng thái sẵn sàng." }
Write-Host "LMSPilot đã sẵn sàng tại http://localhost:3000"
