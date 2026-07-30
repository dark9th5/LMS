$ErrorActionPreference = "Stop"
Set-Location (Resolve-Path "$PSScriptRoot\..")
python scripts/validate-repository.py
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
node scripts/check-typescript.js
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
python -m unittest discover -s tests -p 'test_*.py' -v
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "STATIC TEST SUITE PASSED"
