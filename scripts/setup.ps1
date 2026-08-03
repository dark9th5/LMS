$ErrorActionPreference = "Stop"
$env:DOCKER_API_VERSION = "1.48"
Set-Location (Resolve-Path "$PSScriptRoot\..")
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw "Docker Desktop chưa được cài đặt." }
docker compose version | Out-Null

if (-not (Test-Path .env)) {
  Copy-Item .env.example .env
  function New-Token([int]$Bytes = 48) {
    $data = New-Object byte[] $Bytes
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($data)
    return [Convert]::ToBase64String($data).Replace('+','-').Replace('/','_').TrimEnd('=')
  }
  $values = @{
    LMSPILOT_JWT_SECRET = New-Token 64
    LMSPILOT_INTERNAL_TOKEN = New-Token 56
    LMSPILOT_DEFAULT_ADMIN_PASSWORD = "Lp-$(New-Token 18)-A9!"
    POSTGRES_ADMIN_PASSWORD = New-Token 24
    POSTGRES_SERVICE_PASSWORD = New-Token 24
    RABBITMQ_DEFAULT_PASS = New-Token 24
    REDIS_PASSWORD = New-Token 24
  }
  $content = Get-Content .env
  foreach ($key in $values.Keys) {
    $escaped = [Regex]::Escape($key)
    $content = $content | ForEach-Object { if ($_ -match "^$escaped=") { "$key=$($values[$key])" } else { $_ } }
  }
  $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
  [IO.File]::WriteAllLines((Join-Path (Get-Location) '.env'), $content, $utf8NoBom)
  Write-Host "Đã tạo .env với secret ngẫu nhiên. Mật khẩu demo nằm tại LMSPILOT_DEFAULT_ADMIN_PASSWORD."
} else {
  Write-Host "Giữ nguyên file .env hiện có."
}

& "$PSScriptRoot\preflight.ps1"
docker compose up -d --build
& "$PSScriptRoot\smoke-test.ps1"
Write-Host "LMSPilot đã sẵn sàng: http://localhost:3000"
