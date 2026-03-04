$ErrorActionPreference="Stop"

Write-Host "=== Compose status ===" -ForegroundColor Cyan
docker compose ps

$APP = docker compose ps -q app
$PG  = docker compose ps -q postgres
if(-not $APP){ throw "compose service 'app' not found/running (docker compose ps -q app empty)" }
if(-not $PG){  throw "compose service 'postgres' not found/running (docker compose ps -q postgres empty)" }

$APP_STATUS = docker inspect -f '{{.State.Status}}' $APP
$PG_STATUS  = docker inspect -f '{{.State.Status}}' $PG
Write-Host "app     status = $APP_STATUS"
Write-Host "postgres status = $PG_STATUS"

if($PG_STATUS -ne "running"){ docker logs --tail 120 $PG; throw "postgres is not running" }
if($APP_STATUS -ne "running"){ docker logs --tail 200 $APP; throw "app is not running" }

$portLine = (docker port $APP 8080/tcp | Select-Object -First 1)
if(-not $portLine){ throw "No port mapping found for app (8080/tcp). Check docker-compose.yml ports." }
$APP_HOST_PORT = [int]($portLine -replace '.*:','')
Write-Host "App host port detected: $APP_HOST_PORT" -ForegroundColor Green

Write-Host "`n=== HTTP checks ===" -ForegroundColor Cyan
curl.exe -fsS "http://127.0.0.1:$APP_HOST_PORT/actuator/health" | Out-Host

Write-Host "`n=== Postgres checks ===" -ForegroundColor Cyan
docker exec $PG pg_isready -U postgres -d rag | Out-Host
docker exec $PG psql -U postgres -d rag -tAc "select current_database(), current_user;" | Out-Host
docker exec $PG psql -U postgres -d rag -tAc "select tablename from pg_tables where schemaname='public' order by tablename;" | Out-Host

$fly = (docker exec $PG psql -U postgres -d rag -tAc "select to_regclass('public.flyway_schema_history');").Trim()
if($fly -and $fly -ne "") {
  Write-Host "`nFlyway history (latest 10):" -ForegroundColor Cyan
  docker exec $PG psql -U postgres -d rag -tAc "select installed_rank, version, description, success from flyway_schema_history order by installed_rank desc limit 10;" | Out-Host
} else {
  Write-Host "Flyway table not found: flyway_schema_history (OK if not using Flyway)" -ForegroundColor Yellow
}

Write-Host "`n✅ ALL CHECKS PASSED" -ForegroundColor Green
Write-Host "Open: http://localhost:$APP_HOST_PORT/actuator/health" -ForegroundColor Cyan
