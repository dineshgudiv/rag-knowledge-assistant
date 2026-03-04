param(
    [int]$PostgresHostPort = 5432
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

function Write-Step([string]$msg) {
    Write-Host "[dev-up] $msg"
}

function Wait-DockerEngine {
    $dockerDesktopExe = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"

    for ($i = 0; $i -lt 2; $i++) {
        docker version *> $null
        if ($LASTEXITCODE -eq 0) {
            return
        }

        if ($i -eq 0) {
            Write-Step "Docker engine unreachable; running WSL shutdown and restarting Docker Desktop"
            wsl --shutdown
            if (Test-Path $dockerDesktopExe) {
                Start-Process -FilePath $dockerDesktopExe | Out-Null
            }
        }

        Write-Step "Waiting for Docker engine"
        for ($j = 0; $j -lt 60; $j++) {
            Start-Sleep -Seconds 2
            docker version *> $null
            if ($LASTEXITCODE -eq 0) {
                return
            }
        }
    }

    throw "Docker engine is not reachable. Start Docker Desktop and retry."
}

function Test-PortFree([int]$Port) {
    $listener = $null
    try {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
        $listener.Start()
        return $true
    }
    catch {
        return $false
    }
    finally {
        if ($listener -ne $null) {
            $listener.Stop()
        }
    }
}

function Select-AppHostPort {
    if ($env:APP_HOST_PORT) {
        $requested = [int]$env:APP_HOST_PORT
        if (Test-PortFree $requested) {
            return $requested
        }
        throw "APP_HOST_PORT=$requested is not available."
    }

    foreach ($candidate in 8081..8099) {
        if (Test-PortFree $candidate) {
            return $candidate
        }
    }
    throw "No free host port found in range 8081..8099."
}

function Remove-StaleComposeContainers {
    $names = @(
        "rag-knowledge-assistant-postgres-1",
        "rag-knowledge-assistant-app-1"
    )
    foreach ($name in $names) {
        cmd /c "docker rm -f $name >nul 2>nul"
    }
}

Push-Location $repoRoot
try {
    Wait-DockerEngine

    $appHostPort = Select-AppHostPort
    $env:APP_HOST_PORT = "$appHostPort"
    $env:POSTGRES_HOST_PORT = "$PostgresHostPort"

    Write-Step "Using APP_HOST_PORT=$($env:APP_HOST_PORT), POSTGRES_HOST_PORT=$($env:POSTGRES_HOST_PORT)"

    Write-Step "Stopping existing compose stack"
    cmd /c "docker compose down -v --remove-orphans >nul 2>nul"
    Remove-StaleComposeContainers

    Write-Step "Building and starting compose stack"
    docker compose up -d --build
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up failed"
    }

    $healthUrl = "http://localhost:$($env:APP_HOST_PORT)/actuator/health"
    $docsUrl = "http://localhost:$($env:APP_HOST_PORT)/v1/documents"
    $evalUrl = "http://localhost:$($env:APP_HOST_PORT)/v1/eval/runs"

    & (Join-Path $PSScriptRoot "wait_health.ps1") -Url $healthUrl -TimeoutSec 300

    Write-Step "Health is UP"
    Write-Host "App health: $healthUrl"
    Write-Host "Documents endpoint: $docsUrl"
    Write-Host "Eval runs endpoint: $evalUrl"
    Write-Host "Postgres host port: 127.0.0.1:$($env:POSTGRES_HOST_PORT)"

    Start-Process $healthUrl
    Start-Process $docsUrl
    Start-Process $evalUrl
}
finally {
    Pop-Location
}
