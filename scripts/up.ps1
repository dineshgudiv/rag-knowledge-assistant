param(
    [switch]$Docker
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

function Write-Step([string]$msg) {
    Write-Host "[up] $msg"
}

function Stop-PortListeners([int[]]$Ports) {
    foreach ($port in $Ports) {
        $lines = netstat -ano | Select-String ":$port"
        $procIds = @()
        foreach ($line in $lines) {
            $parts = ($line.ToString() -split "\s+") | Where-Object { $_ -ne "" }
            if ($parts.Length -gt 0) {
                $candidate = $parts[-1]
                if ($candidate -match "^\d+$") {
                    $procIds += [int]$candidate
                }
            }
        }
        $procIds = $procIds | Select-Object -Unique
        foreach ($procId in $procIds) {
            try {
                Stop-Process -Id $procId -Force -ErrorAction Stop
            } catch {
            }
        }
    }
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
    if ($Docker) {
        Write-Step "Starting Docker mode"
        cmd /c "docker compose down -v --remove-orphans >nul 2>nul"
        Remove-StaleComposeContainers
        docker compose up -d --build
        if ($LASTEXITCODE -ne 0) {
            throw "docker compose up failed"
        }
        & (Join-Path $PSScriptRoot "wait_health.ps1") -Url "http://127.0.0.1:8081/actuator/health" -TimeoutSec 300
        Write-Host "Docker app: http://127.0.0.1:8081/"
        exit 0
    }

    Write-Step "Starting local mode"
    Stop-PortListeners -Ports @(8080, 8081)

    $mvnw = Join-Path $repoRoot "mvnw.cmd"
    if (-not (Test-Path $mvnw)) {
        throw "mvnw.cmd not found"
    }

    $proc = Start-Process -FilePath $mvnw -ArgumentList "spring-boot:run" -PassThru -WorkingDirectory $repoRoot
    Write-Step "Started local app process id $($proc.Id)"

    $deadline = (Get-Date).AddSeconds(120)
    $healthUrl = "http://127.0.0.1:8080/actuator/health"
    while ((Get-Date) -lt $deadline) {
        if ($proc.HasExited) {
            throw "Local app process exited early with code $($proc.ExitCode)"
        }
        try {
            $health = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 3
            if ($health.status -eq "UP") {
                Write-Host "Local app: http://127.0.0.1:8080/"
                exit 0
            }
        } catch {
        }
        Start-Sleep -Seconds 1
    }
    throw "Local health check timeout at $healthUrl"
}
finally {
    Pop-Location
}
