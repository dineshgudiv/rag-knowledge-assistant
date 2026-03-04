param()
$ErrorActionPreference = "Stop"
try {
    $h = Invoke-RestMethod -Uri "http://127.0.0.1:8080/actuator/health" -TimeoutSec 2
    if ($h.status -eq "UP") {
        Write-Host "health=UP"
        $req = '{"question":"quick smoke","topK":1,"minScore":0.9}'
        $resp = & curl.exe -sS -X POST -H "Content-Type: application/json" -d $req http://127.0.0.1:8080/v1/chat/ask
        Write-Host $resp
        exit 0
    }
} catch {}
Write-Host "app not ready"
exit 1
