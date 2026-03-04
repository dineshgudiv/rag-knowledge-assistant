# RUNBOOK

## 1) Build and start

Docker Desktop note:
- Ensure Docker Desktop memory is at least `4 GB` for large PDF ingestion.

```powershell
cd C:\Users\91889\Music\projects\rag-knowledge-assistant
docker compose up -d --build
```

## 2) Run hard check

The script uploads a PDF, runs ask queries with PowerShell JSON posting, checks health endpoints, and performs a basic PII scan on citation snippets.

Default file behavior:
- Uses `-FilePath` when provided.
- Otherwise picks newest PDF from `$HOME\Downloads`.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\hard_check.ps1 -BaseUrl "http://localhost:8081"
```

With explicit file:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\hard_check.ps1 -BaseUrl "http://localhost:8081" -FilePath "C:\Users\you\Downloads\sample.pdf"
```

Expected end of output:

```text
OVERALL: PASS
```

Exit code should be `0`.

## 3) Logs with request_id

```powershell
docker logs -f rag-knowledge-assistant-app-1
```

Filter for request ids:

```powershell
docker logs rag-knowledge-assistant-app-1 2>&1 | Select-String "request_id="
```
