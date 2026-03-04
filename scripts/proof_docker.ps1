param()
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$proofPath = Join-Path $repoRoot "PROOF_DOCKER.md"
$waitHealth = Join-Path $PSScriptRoot "wait_health.ps1"
Push-Location $repoRoot
try {
    & cmd.exe /c "docker compose down -v --remove-orphans >nul 2>&1"
    & cmd.exe /c "docker compose up -d --build"
    if ($LASTEXITCODE -ne 0) { throw "docker compose up failed" }

    $up = $true
    try {
        & powershell -NoProfile -ExecutionPolicy Bypass -File $waitHealth -Url "http://127.0.0.1:8081/actuator/health/readiness" -TimeoutSec 300
        if ($LASTEXITCODE -ne 0) { throw "wait_health returned non-zero exit code" }
    } catch {
        $up = $false
    }

    $ps = (& cmd.exe /c "docker compose ps") | Out-String
    $logs = (& cmd.exe /c "docker compose logs --tail 80 app") | Out-String

    if (-not $up) {
        @"
# Docker Proof

## compose ps
```
$ps
```

## app logs tail 80
```
$logs
```

## health
FAILED
"@ | Set-Content -Encoding utf8 $proofPath
        Write-Host "[proof-docker] health check failed"
        exit 1
    }

    @"
# Docker Proof

## compose ps
```
$ps
```

## app logs tail 80
```
$logs
```

## health
UP
"@ | Set-Content -Encoding utf8 $proofPath

    Write-Host "[proof-docker] PASS"
    Write-Host "App: http://127.0.0.1:8081"
    Write-Host "Health: http://127.0.0.1:8081/actuator/health"
}
finally {
    Pop-Location
}
