# RAG Knowledge Assistant
## Screenshots

### Upload + Demo
![Upload](docs/images/ui_upload.png

### Ask (RAG) with citations
![Ask](docs/images/ui_ask_with_citations.png

### Documents
![Documents](docs/images/ui_documents.png

### Audit trail
![Audit](docs/images/ui_audit.png

### Runtime proof (hard_check)
![Proof](docs/images/proof_hard_check_pass.png)

Spring Boot scaffold for a retrieval-augmented generation (RAG) assistant with ingestion, retrieval, chat, feedback, and evaluation endpoints.

## Run Locally (H2)

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1
```

This starts the app with the default H2 profile.

## Run in Docker (Postgres)

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev_up.ps1
```

This will:
- ensure Docker engine is reachable (with Docker Desktop recovery attempt)
- auto-pick `APP_HOST_PORT` from `8081..8099` (default `8081`)
- run `docker compose down --remove-orphans`
- run `docker compose up -d --build`
- verify `http://localhost:${APP_HOST_PORT}/actuator/health` returns `UP`

Inside Docker, the app connects to PostgreSQL host `postgres` (the Compose service name), not `localhost`.

App runs on `http://localhost:8081` by default.

Stop Docker stack:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev_down.ps1
```

## Postgres Access

Postgres is not an HTTP service. Do not open `localhost:5432` in a browser.
Use `psql`/pgAdmin instead.

Example:

```powershell
docker compose exec postgres psql -U postgres -d rag
```

## Doctor Script

```powershell
.\scripts\doctor.ps1 -Fix
```

Optional destructive Docker WSL reset:

```powershell
.\scripts\doctor.ps1 -Fix -NukeDockerWsl
```

## API Docs

Swagger UI: `http://localhost:8081/swagger-ui/index.html`

## Proof Commands

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\proof_e2e.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\proof_docker.ps1
```

## CI

GitHub Actions workflow:
- `.github/workflows/ci.yml`
- Runs `./mvnw -q test`
- Builds Docker image
- Uploads `target/surefire-reports` as artifact

## Documentation

- `docs/HLD.md`
- `docs/RUNBOOK.md`
- `docs/SECURITY.md`
- `docs/EVAL.md`
