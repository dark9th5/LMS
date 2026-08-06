param(
  [switch]$Build
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $Root

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw "Docker chua duoc cai hoac chua co trong PATH."
}
docker info *> $null
if ($LASTEXITCODE -ne 0) { throw "Docker Engine chua chay." }
if (-not (Test-Path ".env")) {
  throw "Thieu file .env. Hay sao chep .env.example thanh .env"
}

if (-not $env:COMPOSE_PARALLEL_LIMIT) { $env:COMPOSE_PARALLEL_LIMIT = "8" }
$timeout = if ($env:LMSPILOT_STARTUP_TIMEOUT) { $env:LMSPILOT_STARTUP_TIMEOUT } else { "360" }

if ($Build) {
  Write-Host "Dang bien dich lai Docker Image tu ma nguon..."
  docker compose build api-gateway web
  if ($LASTEXITCODE -ne 0) { throw "Build image that bai." }
} else {
  Write-Host "Dang kiem tra va khoi dong he thong tu Docker Image..."
  docker compose pull --ignore-build-failures
}

Write-Host "Dang khoi chay he thong LMSPilot..."
docker compose up -d --remove-orphans --wait --wait-timeout $timeout
if ($LASTEXITCODE -ne 0) { throw "Mot hoac nhieu service chua san sang." }
Write-Host "LMSPilot da san sang tai http://localhost:3000"
