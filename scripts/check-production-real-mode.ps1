$ErrorActionPreference = "Stop"
node .\scripts\check-production-real-mode.mjs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
