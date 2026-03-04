param(
    [string]$Url = "http://127.0.0.1:8081/actuator/health/readiness",
    [int]$TimeoutSec = 90
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Write-Host "[wait-health] Waiting for $Url (timeout ${TimeoutSec}s)"
$deadline = (Get-Date).AddSeconds($TimeoutSec)
$fallbackUrl = $Url -replace "/actuator/health/readiness$", "/actuator/health"

while ((Get-Date) -lt $deadline) {
    try {
        $response = Invoke-RestMethod -Uri $Url -TimeoutSec 3
        if ($null -ne $response -and $response.status -eq "UP") {
            Write-Host "[wait-health] Health is UP"
            exit 0
        }
    } catch {
        try {
            $fallback = Invoke-RestMethod -Uri $fallbackUrl -TimeoutSec 3
            if ($null -ne $fallback -and $fallback.status -eq "UP") {
                Write-Host "[wait-health] Health is UP (fallback)"
                exit 0
            }
        } catch {
        }
    }
    Start-Sleep -Seconds 1
}

Write-Host "[wait-health] Timed out waiting for health"
Push-Location $repoRoot
try {
    docker compose ps
    docker compose logs --tail 200 app
} finally {
    Pop-Location
}
throw "Health check timeout for $Url"
