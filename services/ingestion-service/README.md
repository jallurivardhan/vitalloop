# ingestion-service

**Status:** Phase 3 — event-driven. Validates a reading, publishes a
`vitals.reading.received` event to Pub/Sub, and returns `202 Accepted`. Triage
now runs asynchronously in the [`triage-agent-service`](../triage-agent-service/README.md).

## Purpose

Accepts vital-sign readings, validates them, enriches them (defaults
`recordedAt`, generates an `eventId`), and publishes a `VitalsReadingReceived`
event. Built with a **ports-and-adapters (hexagonal)** architecture so the
transport (Pub/Sub) sits behind an `EventPublisher` port.

The build and tests run with **no GCP credentials** (the Pub/Sub emulator host
is configured by default, so the Spring context loads without connecting).

## Architecture (hexagonal)

```
adapter/web  ──▶ application ──▶ port (EventPublisher) ◀── adapter/messaging
  (REST in)        (use case)       (interface)             (PubSubTemplate)
                       │
                     domain  (VitalsReading; framework-free)
                       ▲
                     config  (Spring wiring)
```

- `domain/` — `VitalsReading` (the inbound model, with Bean Validation).
- `application/` — `IngestionService` (enriches and publishes) and
  `IngestionAccepted` (the result). Framework-free; wired in `config/`.
- `port/` — `EventPublisher` interface the application depends on.
- `adapter/web/` — inbound REST adapter + structured error handling.
- `adapter/messaging/` — `PubSubEventPublisher` (publishes event JSON to the
  `vitals-reading-received` topic via `PubSubTemplate`).
- `config/` — Spring configuration wiring the application layer.

## Running locally

Requires **JDK 17**. The end-to-end async loop needs the local infra:

```bash
make dev-up                       # starts the Pub/Sub emulator (and other infra)
mvn -B -pl services/ingestion-service spring-boot:run \
  -Dspring-boot.run.profiles=local
```

The service listens on **http://localhost:8081**. Actuator health/liveness/
readiness probes are exposed under `/actuator`; console logging is structured
JSON (ECS).

## Endpoint

### `POST /api/v1/vitals`

Accepts a single reading for asynchronous triage. Returns `202 Accepted`.

**Request body**

```json
{
  "patientId": "patient-1",
  "vitalType": "blood_pressure_systolic",
  "value": 185.0,
  "unit": "mmHg",
  "recordedAt": "2026-06-16T12:00:00Z"
}
```

- `patientId`, `vitalType`, `unit` — required, non-blank.
- `value` — required, positive.
- `recordedAt` — optional; defaults to the current time when omitted.

**Response `202`**

```json
{ "eventId": "0b8f1d2e-..." }
```

The `eventId` correlates this reading with the downstream triage result.

**Validation error `400`** — structured body with `fieldErrors` (unchanged).

### Example

```bash
curl -i -X POST http://localhost:8081/api/v1/vitals \
  -H 'Content-Type: application/json' \
  -d '{"patientId":"p-1","vitalType":"spo2","value":86,"unit":"%"}'
```

## Configuration

```yaml
spring:
  cloud:
    gcp:
      project-id: ${PUBSUB_PROJECT_ID:vitalloop-local}
      pubsub:
        emulator-host: ${PUBSUB_EMULATOR_HOST:localhost:8085}
vitalloop:
  pubsub:
    topic: vitals-reading-received
```

## Tests

```bash
mvn -B -pl services/ingestion-service verify
```

- `IngestionServiceTest` — enrichment + publish via a **mocked `EventPublisher`**
  (defaulted `recordedAt`, preserved `recordedAt`, returned `eventId`).
- `IngestionControllerTest` — `@WebMvcTest` with a mocked service: `202` with the
  `eventId` on success, `400` with `fieldErrors` on an invalid body.
- `IngestionApplicationTests` — Spring context smoke test.
- `PubSubEventPublisherIntegrationTest` — publishes to the real emulator.
  **Excluded by default** (`@Tag("integration")` + an `@EnabledIfEnvironmentVariable`
  gate on `PUBSUB_EMULATOR_HOST`). To run it with `make dev-up` active:

  ```bash
  $env:PUBSUB_EMULATOR_HOST="localhost:8085"
  mvn -B -pl services/ingestion-service verify -DexcludedGroups= -Dgroups=integration
  ```

See [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md) for full platform context.
