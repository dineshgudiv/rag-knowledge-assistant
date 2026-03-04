# Security

## Request Identity
- Every request gets `X-Request-Id` via `RequestIdFilter`
- Incoming `X-Request-Id` is accepted when present
- `request_id` is propagated through MDC and response payloads

## API Key Gate (Optional)
- Controlled by `app.security.api-key` (or env mapping)
- If empty: gate is disabled
- If set: requires header `X-API-KEY`
- On success, actor is tagged as `apiKey:<hash-prefix>` in audit logs

## Prompt Injection Guard
- `PromptInjectionDetector` blocks common jailbreak/instruction override patterns
- Blocked asks return:
  - `answer: "I don't know."`
  - `mode: "blocked"`
  - no citations
- Block reason is audited

## Data Governance
- Audit logs capture action, actor, route, request_id, docs/chunks touched, and block flags
- Feedback endpoint validates `queryLogId` existence before write

## Error Handling
- Error JSON includes `error`, `code`, `request_id`, `path`, `timestamp`
- Stack traces are not returned to clients