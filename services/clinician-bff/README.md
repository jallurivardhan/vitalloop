# clinician-bff

**Status:** Placeholder — implementation coming in Phase 2.

## Responsibility

Backend For Frontend service for the React clinician dashboard. Aggregates
data from the FHIR store and other services, provides REST and WebSocket
endpoints, and handles clinician authentication via Google Identity / OAuth 2.0.
The only internet-facing service in the platform.

## Planned tech

- Spring Boot 3.4.x
- Spring WebFlux (reactive WebSocket support)
- Spring Security (OAuth 2.0 resource server)
- Spring Cloud GCP Pub/Sub
- HAPI FHIR Client (R4)

## Exposed endpoints (planned)

| Method | Path | Description |
|---|---|---|
| GET | `/api/patients` | List enrolled patients |
| GET | `/api/patients/{id}/vitals` | Patient vital-sign history |
| WS | `/ws/vitals` | Real-time vital-sign stream |
| GET | `/api/alerts` | Active alert queue |

See [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md) for full context.
