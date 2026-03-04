param(
    [switch]$Fix,
    [switch]$NukeDockerWsl
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "[doctor] $Message"
}

function Install-PortableJdk17 {
    param([string]$RepoRoot)

    $toolsDir = Join-Path $RepoRoot ".tools"
    $zipPath = Join-Path $toolsDir "temurin17.zip"
    $extractDir = Join-Path $toolsDir "_temurin17_extract"
    $jdkDir = Join-Path $toolsDir "jdk17"
    $javaExe = Join-Path $jdkDir "bin\java.exe"

    New-Item -ItemType Directory -Path $toolsDir -Force | Out-Null

    if (-not (Test-Path $javaExe)) {
        Write-Step "Installing portable Temurin JDK 17 into .tools\jdk17"
        $url = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
        Invoke-WebRequest -Uri $url -OutFile $zipPath

        if (Test-Path $extractDir) {
            Remove-Item -Path $extractDir -Recurse -Force
        }
        New-Item -ItemType Directory -Path $extractDir -Force | Out-Null

        Expand-Archive -Path $zipPath -DestinationPath $extractDir -Force

        $topLevel = Get-ChildItem -Path $extractDir | Select-Object -First 1
        if (-not $topLevel) {
            throw "JDK archive extraction produced no files."
        }

        if (Test-Path $jdkDir) {
            Remove-Item -Path $jdkDir -Recurse -Force
        }
        New-Item -ItemType Directory -Path $jdkDir -Force | Out-Null

        Copy-Item -Path (Join-Path $topLevel.FullName "*") -Destination $jdkDir -Recurse -Force
        Remove-Item -Path $extractDir -Recurse -Force
    }

    $env:JAVA_HOME = $jdkDir
    $javaBin = Join-Path $env:JAVA_HOME "bin"
    if (-not ($env:Path -split ';' | Where-Object { $_ -eq $javaBin })) {
        $env:Path = "$javaBin;$env:Path"
    }

    [Environment]::SetEnvironmentVariable("JAVA_HOME", $env:JAVA_HOME, "User")
    Write-Step "JAVA_HOME set to $env:JAVA_HOME"
}

function Test-MavenWrapper {
    param([string]$RepoRoot)

    $mvnw = Join-Path $RepoRoot "mvnw.cmd"
    if (-not (Test-Path $mvnw)) {
        throw "mvnw.cmd not found in repository root."
    }

    Write-Step "Verifying Maven wrapper"
    & $mvnw -v
    if ($LASTEXITCODE -ne 0) {
        throw "mvnw.cmd -v failed."
    }
}

function Set-DockerLifecycleTimeout {
    $settingsPath = Join-Path $env:APPDATA "Docker\settings.json"
    if (-not (Test-Path $settingsPath)) {
        Write-Step "Docker settings file not found at $settingsPath"
        return
    }

    Write-Step "Setting Docker lifecycleTimeoutSeconds to 3600"
    $settings = Get-Content -Raw -Path $settingsPath | ConvertFrom-Json
    $settings | Add-Member -NotePropertyName lifecycleTimeoutSeconds -NotePropertyValue 3600 -Force
    $settings | ConvertTo-Json -Depth 100 | Set-Content -Path $settingsPath -Encoding UTF8
}

function Repair-DockerDesktop {
    param([switch]$DoNuke)

    $dockerDesktopExe = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"

    if ($DoNuke) {
        Write-Step "Destructive Docker WSL reset requested"
        & wsl --shutdown
        & wsl --unregister docker-desktop
        & wsl --unregister docker-desktop-data
        return
    }

    if (-not (Test-Path $dockerDesktopExe)) {
        Write-Step "Docker Desktop is not installed. Skipping Docker repair path."
        return
    }

    Write-Step "Running WSL shutdown for Docker recovery"
    & wsl --shutdown

    Write-Step "Attempting to start Docker Desktop"
    Start-Process -FilePath $dockerDesktopExe | Out-Null
    Start-Sleep -Seconds 5

    $dockerHealthy = $false
    try {
        $null = & docker version --format "{{.Server.Version}}" 2>$null
        if ($LASTEXITCODE -eq 0) {
            $dockerHealthy = $true
        }
    } catch {
        $dockerHealthy = $false
    }

    if (-not $dockerHealthy) {
        Write-Step "Docker still unhealthy. Applying lifecycle timeout repair and retrying."
        Set-DockerLifecycleTimeout
        & wsl --shutdown
        Start-Process -FilePath $dockerDesktopExe | Out-Null
    } else {
        Write-Step "Docker Desktop responded successfully"
    }
}

function Strip-Utf8Bom {
    param([string]$RepoRoot)
    Write-Step "Removing UTF-8 BOM from source/config files"
    $patterns = @("*.java", "*.yml", "*.yaml", "*.sql", "*.xml", "*.md", "*.ps1")
    foreach ($pattern in $patterns) {
        $files = Get-ChildItem -Path $RepoRoot -Recurse -File -Filter $pattern
        foreach ($file in $files) {
            $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
            if ($bytes.Length -ge 3 -and $bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191) {
                if ($bytes.Length -eq 3) {
                    [System.IO.File]::WriteAllBytes($file.FullName, @())
                } else {
                    [System.IO.File]::WriteAllBytes($file.FullName, $bytes[3..($bytes.Length - 1)])
                }
            }
        }
    }
}

if (-not $Fix) {
    Write-Host "Usage: .\scripts\doctor.ps1 -Fix [-NukeDockerWsl]"
    exit 1
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Install-PortableJdk17 -RepoRoot $repoRoot
Strip-Utf8Bom -RepoRoot $repoRoot
Test-MavenWrapper -RepoRoot $repoRoot
Repair-DockerDesktop -DoNuke:$NukeDockerWsl

Write-Step "Doctor completed"
