param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $repoRoot

try {
    Write-Host "[docker-up] Cleaning existing compose stack"
    docker compose down --remove-orphans
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose down failed"
    }

    Write-Host "[docker-up] Building and starting services"
    docker compose up -d --build
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up failed"
    }

    Write-Host "[docker-up] Service status"
    docker compose ps

    Write-Host "[docker-up] Waiting for app startup log line"
    $started = $false
    for ($i = 0; $i -lt 60; $i++) {
        $logs = docker compose logs app --tail 200 2>&1
        if ($logs -match "Started .* in .* seconds") {
            $started = $true
            break
        }
        Start-Sleep -Seconds 2
    }

    if (-not $started) {
        Write-Host "[docker-up] App did not report startup in time"
        docker compose logs app --tail 200
        exit 1
    }

    Write-Host "[docker-up] Checking actuator health"
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:8080/actuator/health" -TimeoutSec 10
    } catch {
        Write-Host "[docker-up] Health endpoint request failed"
        docker compose logs app --tail 200
        exit 1
    }

    if ($health.status -ne "UP") {
        Write-Host "[docker-up] Health status is not UP: $($health | ConvertTo-Json -Compress)"
        docker compose logs app --tail 200
        exit 1
    }

    Write-Host "[docker-up] Health status: $($health.status)"
}
finally {
    Pop-Location
}
