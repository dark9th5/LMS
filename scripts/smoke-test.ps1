$ErrorActionPreference = "Stop"
Set-Location (Resolve-Path "$PSScriptRoot\..")

if (-not (Test-Path .env)) { throw "Thieu .env. Hay chay scripts\setup.ps1 truoc." }

$settings = @{}
Get-Content .env | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
    $parts = $line.Split('=', 2)
    $settings[$parts[0]] = $parts[1]
  }
}

$gateway = if ($env:LMSPILOT_SMOKE_GATEWAY_URL) { $env:LMSPILOT_SMOKE_GATEWAY_URL } else { "http://localhost:8080" }
$web = if ($env:LMSPILOT_SMOKE_WEB_URL) { $env:LMSPILOT_SMOKE_WEB_URL } else { "http://localhost:3000" }
$password = $settings["LMSPILOT_DEFAULT_ADMIN_PASSWORD"]
if (-not $password) { throw "Thieu LMSPILOT_DEFAULT_ADMIN_PASSWORD trong .env" }

function Wait-Http([string]$Url, [string]$Name, [int]$Attempts = 120) {
  for ($i = 1; $i -le $Attempts; $i++) {
    try {
      Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 4 | Out-Null
      Write-Host "READY: $Name"
      return
    } catch {
      Start-Sleep -Seconds 2
    }
  }
  docker compose ps
  throw "TIMEOUT: $Name ($Url)"
}

function Login([string]$Username) {
  $body = @{ username = $Username; password = $password } | ConvertTo-Json -Compress
  return Invoke-RestMethod -Method Post -Uri "$gateway/api/v1/auth/login" -ContentType "application/json" -Body $body -TimeoutSec 15
}

function Auth-Get([string]$Token, [string]$Path) {
  Invoke-RestMethod -Method Get -Uri "$gateway$Path" -Headers @{ Authorization = "Bearer $Token" } -TimeoutSec 20 | Out-Null
  Write-Host "PASS: GET $Path"
}

function Assert-Role($Payload, [string]$Role, [string]$Username) {
  if (-not ($Payload.user.roles -contains $Role)) { throw ("Sai vai tro dang nhap cho " + $Username + ": can " + $Role) }
  Write-Host "PASS: $Username -> $Role"
}

function Web-Login-Check([string]$Username, [string]$Role, [string]$Landing, [string]$ExpectedName) {
  $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
  $body = @{ username = $Username; password = $password } | ConvertTo-Json -Compress
  $payload = Invoke-RestMethod -Method Post -Uri "$web/api/auth/login" -ContentType "application/json" -Body $body -WebSession $session -TimeoutSec 20
  Assert-Role $payload $Role "$Username (Web)"
  $page = Invoke-WebRequest -UseBasicParsing -Uri "$web$Landing" -WebSession $session -TimeoutSec 20
  if ($page.StatusCode -ne 200) { throw "Web $Username mo $Landing nhung tra ve status $($page.StatusCode)" }
  Write-Host "PASS: Web $Username opens $Landing as $ExpectedName ($($page.StatusCode) OK)"
}

Wait-Http "$gateway/actuator/health/readiness" "API Gateway"
Wait-Http "$web/login" "Web portal"

$admin = Login "admin"
if (-not $admin.accessToken) { throw "Dang nhap admin khong tra access token." }
Assert-Role $admin "ADMIN" "admin"
Auth-Get $admin.accessToken "/api/v1/users"
Auth-Get $admin.accessToken "/api/v1/organization/units"
Auth-Get $admin.accessToken "/api/v1/courses"
Auth-Get $admin.accessToken "/api/v1/classes"
Auth-Get $admin.accessToken "/api/v1/reports/dashboard"

$instructor = Login "instructor"
if (-not $instructor.accessToken) { throw "Dang nhap instructor khong tra access token." }
Assert-Role $instructor "INSTRUCTOR" "instructor"
Auth-Get $instructor.accessToken "/api/v1/courses"
Auth-Get $instructor.accessToken "/api/v1/classes"
Auth-Get $instructor.accessToken "/api/v1/questions"
Auth-Get $instructor.accessToken "/api/v1/grades/queue"

$student = Login "student"
if (-not $student.accessToken) { throw "Dang nhap student khong tra access token." }
Assert-Role $student "STUDENT" "student"
Auth-Get $student.accessToken "/api/v1/learning/me"
Auth-Get $student.accessToken "/api/v1/exams"
Auth-Get $student.accessToken "/api/v1/grades/me"
Auth-Get $student.accessToken "/api/v1/certificates/me"

Web-Login-Check "admin" "ADMIN" "/dashboard" "Quản trị hệ thống"
Web-Login-Check "instructor" "INSTRUCTOR" "/dashboard" "Giảng viên mẫu"
Web-Login-Check "student" "STUDENT" "/learning" "Học viên mẫu"

Write-Host "SMOKE TEST PASSED"
