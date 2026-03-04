# High-Level Design

## Overview
RAG Knowledge Assistant is a Spring Boot 3 / Java 17 backend with a static browser UI.
It supports document ingestion, retrieval-augmented Q&A, feedback capture, eval runs, and audit logging.

## Runtime Modes
- Local mode (default): H2 file DB + Flyway migrations
- Docker mode: Postgres 16 + app profile `postgres`

## Core Components
- `controller`: REST endpoints under `/v1/*` and actuator health
- `service`: ingestion, retrieval, rag orchestration, eval, audit
- `repository`: JPA repositories
- `model`: entities and non-entity domain records
- `util`: chunking, hashing, embeddings, vector math, request helpers
- `security`: request-id filter and optional API-key filter

## Data Flow
1. Upload file -> extract text (PDF/TXT/MD)
2. Parent/child chunking + token normalization
3. Embeddings generated (hash by default; ollama optional fallback)
4. Chunks persisted with metadata and scores support fields
5. Ask request performs hybrid retrieval + reranking
6. Response is evidence-only extractive answer or `I don't know.`
7. Query logs, feedback, eval runs, and audit logs persisted

## Retrieval Strategy
- Candidate prefilter from tokens
- Vector similarity + keyword overlap hybrid score
- Reranking by bigram overlap and length heuristics
- Parent expansion for richer snippets
- TopK capped to 5

## Reliability and Ops
- Docker compose health-gated startup (`depends_on: service_healthy`)
- Request IDs in all responses (`X-Request-Id`)
- Probe-based health (`/actuator/health/liveness`, `/actuator/health/readiness`)
- PowerShell scripts for deterministic startup/proof on Windows