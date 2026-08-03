$ErrorActionPreference = "Stop"
$env:DOCKER_API_VERSION = "1.48"
Set-Location (Resolve-Path "$PSScriptRoot\..")
function Fail([string]$Message) { throw "PRECHECK FAILED: $Message" }
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { Fail "Chưa cài Docker Desktop." }
docker compose version | Out-Null
if ($LASTEXITCODE -ne 0) { Fail "Cần Docker Compose v2." }
docker info | Out-Null
if ($LASTEXITCODE -ne 0) { Fail "Docker Engine chưa chạy." }
if (-not (Test-Path .env)) { Fail "Thiếu .env. Hãy chạy scripts/setup.ps1." }

$envValues = @{}
Get-Content .env | ForEach-Object {
  if ($_ -and -not $_.StartsWith('#') -and $_.Contains('=')) {
    $parts = $_.Split('=', 2)
    $envValues[$parts[0]] = $parts[1]
  }
}
$required = @('LMSPILOT_JWT_SECRET','LMSPILOT_INTERNAL_TOKEN','LMSPILOT_DEFAULT_ADMIN_PASSWORD','POSTGRES_ADMIN_PASSWORD','POSTGRES_SERVICE_PASSWORD','RABBITMQ_DEFAULT_PASS','REDIS_PASSWORD')
foreach ($name in $required) {
  $value = [string]$envValues[$name]
  if ([string]::IsNullOrWhiteSpace($value)) { Fail "$name đang trống." }
  if ($value.StartsWith('replace-with-') -or $value.StartsWith('ChangeMe-')) { Fail "$name vẫn đang dùng giá trị mẫu." }
}
if ($envValues['LMSPILOT_JWT_SECRET'].Length -lt 32) { Fail "LMSPILOT_JWT_SECRET phải có ít nhất 32 ký tự." }
if ($envValues['LMSPILOT_INTERNAL_TOKEN'].Length -lt 32) { Fail "LMSPILOT_INTERNAL_TOKEN phải có ít nhất 32 ký tự." }

docker compose config --quiet
if ($LASTEXITCODE -ne 0) { Fail "docker-compose.yml hoặc .env không hợp lệ." }
$drive = Get-PSDrive -Name (Get-Location).Drive.Name
if ($drive.Free -lt 10GB) { Fail "Cần tối thiểu khoảng 10 GB dung lượng trống để build và chạy lần đầu." }
Write-Host "PRECHECK PASSED"
