# Runbook

## Start
### Local (H2)
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\up.ps1
```
App: `http://127.0.0.1:8080/`

### Docker (Postgres)
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\up.ps1 -Docker
```
App: `http://127.0.0.1:8081/`

## Stop
```powershell
docker compose down --remove-orphans
```

## Health Checks
- Liveness: `GET /actuator/health/liveness`
- Readiness: `GET /actuator/health/readiness`
- Fallback: `GET /actuator/health`

Use helper script:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\wait_health.ps1
```

## Common Failures
### H2 file lock
- Symptom: `Database may be already in use`
- Fix: stop duplicate app process; H2 URL already uses `AUTO_SERVER=TRUE`

### Docker app not healthy yet
- Symptom: `Empty reply from server`
- Fix: wait for readiness using `scripts/wait_health.ps1`

### Docker compose conflicts
- Container names are intentionally not pinned; use:
```powershell
docker compose down -v --remove-orphans
```

## Logs
```powershell
docker compose ps
docker compose logs --tail 200 app
docker compose logs --tail 200 postgres
```

## Proof Commands
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\proof_e2e.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\proof_docker.ps1
```