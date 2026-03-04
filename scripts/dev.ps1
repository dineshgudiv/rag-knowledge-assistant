param(
    [switch]$UseDockerPg,
    [switch]$NukeDockerWsl
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$doctor = Join-Path $PSScriptRoot "doctor.ps1"
$mvnw = Join-Path $repoRoot "mvnw.cmd"

if ($NukeDockerWsl) {
    & powershell -NoProfile -ExecutionPolicy Bypass -File $doctor -Fix -NukeDockerWsl
} else {
    & powershell -NoProfile -ExecutionPolicy Bypass -File $doctor -Fix
}
if ($LASTEXITCODE -ne 0) {
    throw "Doctor step failed"
}

Push-Location $repoRoot
try {
    & $mvnw -q test
    if ($LASTEXITCODE -ne 0) {
        throw "Tests failed"
    }

    if ($UseDockerPg) {
        Write-Host "[dev] Starting PostgreSQL with docker compose"
        & docker compose up -d postgres
        if ($LASTEXITCODE -ne 0) {
            throw "docker compose up failed"
        }
        $env:SPRING_PROFILES_ACTIVE = "postgres"
        Write-Host "[dev] SPRING_PROFILES_ACTIVE=postgres"
    } else {
        Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
        Write-Host "[dev] Starting with default H2 profile"
    }

    & $mvnw spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "Application failed to start"
    }
}
finally {
    Pop-Location
}
