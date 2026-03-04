param()

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Push-Location $repoRoot
try {
    docker compose down --remove-orphans
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose down failed"
    }
    Write-Host "[dev-down] Compose stack stopped"
}
finally {
    Pop-Location
}
