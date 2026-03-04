param()
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$doctor = Join-Path $PSScriptRoot "doctor.ps1"
$mvnw = Join-Path $repoRoot "mvnw.cmd"
$proofPath = Join-Path $repoRoot "PROOF.md"
$samplePath = Join-Path $repoRoot "sample.txt"
$appOut = Join-Path $repoRoot "proof_app.log"
$appErr = Join-Path $repoRoot "proof_app.err.log"

function Fail($msg) {
    Write-Host "[proof-e2e] FAIL: $msg"
    exit 1
}

function Post-Json($url, $bodyObj) {
    $body = $bodyObj | ConvertTo-Json -Compress
    return Invoke-RestMethod -Method Post -Uri $url -ContentType "application/json" -Body $body
}

function Post-MultipartFile($url, $filePath) {
    if (-not (Test-Path $filePath)) {
        throw "File not found: $filePath"
    }
    $raw = & curl.exe -sS -X POST -F "file=@$filePath" $url
    if ($LASTEXITCODE -ne 0) {
        throw "curl multipart upload failed"
    }
    return $raw | ConvertFrom-Json
}

Push-Location $repoRoot
$proc = $null
try {
    & powershell -NoProfile -ExecutionPolicy Bypass -File $doctor -Fix
    if ($LASTEXITCODE -ne 0) { Fail "doctor failed" }

    & $mvnw -q test
    if ($LASTEXITCODE -ne 0) { Fail "tests failed" }

    Get-CimInstance Win32_Process | Where-Object {
        $_.Name -match "java|cmd|powershell" -and $_.CommandLine -match "spring-boot:run|rag-knowledge-assistant"
    } | ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }

    Get-ChildItem -Path (Join-Path $repoRoot ".data") -Filter "ragdb*" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue

    if (Test-Path $appOut) { Remove-Item $appOut -Force -ErrorAction SilentlyContinue }
    if (Test-Path $appErr) { Remove-Item $appErr -Force -ErrorAction SilentlyContinue }

    $proc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "`"$mvnw`" spring-boot:run" `
        -WorkingDirectory $repoRoot -RedirectStandardOutput $appOut -RedirectStandardError $appErr -PassThru

    $up = $false
    for ($i = 0; $i -lt 120; $i++) {
        Start-Sleep -Seconds 1
        try {
            $h = Invoke-RestMethod -Uri "http://127.0.0.1:8080/actuator/health" -TimeoutSec 2
            if ($h.status -eq "UP") { $up = $true; break }
        } catch {}
    }
    if (-not $up) {
        if (Test-Path $appOut) { Get-Content $appOut -Tail 200 | Write-Host }
        if (Test-Path $appErr) { Get-Content $appErr -Tail 200 | Write-Host }
        Fail "health endpoint not UP in 120s"
    }

    @'
RAG means retrieval augmented generation.
This system returns citations in answers.
'@ | Set-Content -Encoding utf8 $samplePath

    $upload = Post-MultipartFile "http://127.0.0.1:8080/v1/documents/upload" $samplePath
    if (-not $upload.documentId -or [int]$upload.chunkCount -le 0) { Fail "upload assertion failed" }

    $inRes = Post-Json "http://127.0.0.1:8080/v1/chat/ask" @{ question = "What does RAG mean?"; topK = 3; minScore = 0.05 }
    if (-not $inRes.citations -or $inRes.citations.Count -le 0) { Fail "in-scope citations assertion failed" }

    $outRes = Post-Json "http://127.0.0.1:8080/v1/chat/ask" @{ question = "What is the capital of Mars?"; topK = 3; minScore = 0.95 }
    if ($outRes.answer -ne "I don't know.") { Fail "out-of-scope IDK assertion failed" }

    $evalRes = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/v1/eval/run"
    if ($null -eq $evalRes.score) { Fail "eval score assertion failed" }

    $javaExe = "java"
    $javaHomeExe = Join-Path $env:JAVA_HOME "bin\\java.exe"
    if ($env:JAVA_HOME -and (Test-Path $javaHomeExe)) { $javaExe = $javaHomeExe }
    $javaVersion = (& cmd.exe /c "`"$javaExe`" -version 2>&1" | Out-String)
    $mvnVersion = (& cmd.exe /c "`"$mvnw`" -v 2>&1" | Out-String)

    $proofLines = @(
        "# Proof E2E",
        "",
        "## java -version",
        '```',
        $javaVersion.TrimEnd(),
        '```',
        "",
        "## mvnw -v",
        '```',
        $mvnVersion.TrimEnd(),
        "```",
        "",
        "## upload",
        '```json',
        ($upload | ConvertTo-Json -Depth 8),
        '```',
        "",
        "## ask in-scope",
        '```json',
        ($inRes | ConvertTo-Json -Depth 8),
        '```',
        "",
        "## ask out-of-scope",
        '```json',
        ($outRes | ConvertTo-Json -Depth 8),
        '```',
        "",
        "## eval",
        '```json',
        ($evalRes | ConvertTo-Json -Depth 8),
        '```'
    )
    $proofLines -join "`r`n" | Set-Content -Encoding utf8 $proofPath

    Write-Host "[proof-e2e] PASS"
}
catch {
    if (Test-Path $appOut) { Get-Content $appOut -Tail 120 | Write-Host }
    if (Test-Path $appErr) { Get-Content $appErr -Tail 120 | Write-Host }
    Fail $_.Exception.Message
}
finally {
    if ($proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
    Get-CimInstance Win32_Process | Where-Object {
        $_.Name -match "java|cmd" -and $_.CommandLine -match "spring-boot:run|rag-knowledge-assistant"
    } | ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
    Pop-Location
}
