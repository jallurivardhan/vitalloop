# triage-agent-service

**Status:** Phase 3 — event-driven. Subscribes to `vitals.reading.received`
events and runs the triage pipeline. Hosts the triage stack moved out of
`ingestion-service`.

## Purpose

Consumes `VitalsReadingReceived` events from Pub/Sub, reconstructs the reading,
classifies it with a pluggable `TriageEngine`, and forwards the decision to a
`TriageResultHandler`. Built with a **ports-and-adapters (hexagonal)**
architecture; the triage engine swaps from the deterministic rule-based stub to
the AI-backed ADK engine by configuration alone.

The build and tests run with **no GCP credentials** (the Pub/Sub emulator host
is configured by default, and the live subscriber / topic bootstrap are
`local`-profile only).

## Architecture (hexagonal)

```
adapter/messaging ──▶ application ──▶ port (TriageEngine)       ◀── adapter/triage
  (Pub/Sub in)          (use case)     port (TriageResultHandler) ◀── adapter/result
                            │
                          domain  (VitalsReading, TriageDecision, … no framework)
                            ▲
                          config  (Spring wiring + local Pub/Sub bootstrap)
```

- `domain/` — `VitalsReading`, `TriageDecision`, `CarePlanThresholds`,
  `DefaultCarePlanThresholds`, and the `Severity` / `RecommendedAction` enums.
- `application/` — `TriageService` (triage + hand off). Framework-free.
- `port/` — `TriageEngine` and `TriageResultHandler` interfaces.
- `adapter/messaging/` — `VitalsEventConsumer` (message-handling logic, unit
  tested) and `PubSubSubscriberInitializer` (`local`-only live subscription).
- `adapter/triage/` — `RuleBasedTriageEngine` (default), `AdkTriageEngine`,
  `AdkAgentConfig`, `CarePlanTools`, `TriageDecisionParser`.
- `adapter/result/` — `LoggingTriageResultHandler` (logs the decision as JSON).
- `config/` — Spring wiring + `PubSubLocalInitializer` (create-if-absent topic
  and subscription on the emulator).

## Running locally

Requires **JDK 17** and the local infra. Run under the `local` profile so the
topic/subscription are created and the subscriber starts:

```bash
make dev-up
mvn -B -pl services/triage-agent-service spring-boot:run \
  -Dspring-boot.run.profiles=local
```

The service listens on **http://localhost:8082** (actuator health/liveness/
readiness under `/actuator`; structured JSON logging). Post a reading to the
ingestion service and watch the triage decision appear in this service's logs.

## Triage engine toggle

```yaml
vitalloop:
  triage:
    engine: rule-based   # default; deterministic stub. Or 'adk'.
    model: gemini-2.5-flash
```

| Value | Engine | Behaviour |
|---|---|---|
| `rule-based` (default) | `RuleBasedTriageEngine` | Deterministic. No GCP needed. |
| `adk` | `AdkTriageEngine` | Google ADK + Gemini on Vertex AI. Requires GCP. |

Selected via `@ConditionalOnProperty` — the application layer and tests are
untouched. With `rule-based`, **no ADK beans load and no Vertex connection is
attempted**.

### Running the real ADK agent (Vertex AI)

> **Cost warning:** the `adk` engine calls Gemini on Vertex AI, which incurs
> Google Cloud charges per request.

```bash
gcloud auth application-default login
export GOOGLE_GENAI_USE_VERTEXAI=true
export GOOGLE_CLOUD_PROJECT=<your-gcp-project-id>
export GOOGLE_CLOUD_LOCATION=us-central1

mvn -B -pl services/triage-agent-service spring-boot:run \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.arguments=--vitalloop.triage.engine=adk
```

## Tests

```bash
mvn -B -pl services/triage-agent-service verify
```

- `VitalsEventConsumerTest` — feeds a sample event through the consumer and
  asserts the (mocked) engine is invoked and the (mocked) handler is called, and
  that a malformed payload is nacked without invoking the engine.
- `RuleBasedTriageEngineTest` — every severity band, boundary values, and the
  escalate-on-uncertainty rule.
- `TriageDecisionParserTest` — parses clean/fenced JSON and the safe fallback.
- `TriageAgentApplicationTests` — Spring context smoke test.

Tests tagged `@Tag("integration")` are **excluded from the default build** and run
only via the opt-in `integration` profile, which flips the Surefire gating:

```bash
mvn -B -Pintegration -pl services/triage-agent-service -am test
```

- `AdkTriageEngineIntegrationTest` — live Vertex AI test (also `@EnabledIfEnvironmentVariable`
  on `GOOGLE_CLOUD_PROJECT`, so it is skipped without credentials).
- `PubSubEndToEndIntegrationTest` — **requires Docker**. Starts a Pub/Sub emulator
  via Testcontainers, boots the app under the `local` profile, publishes a CRITICAL
  and a NORMAL `vitals.reading.received` event, and asserts the consumer produces
  `PHYSICIAN_ESCALATION` / `NONE` (correlated by `eventId` via the MDC the consumer
  sets). It also guards the startup-ordering fix: the topic/subscription must exist
  before the subscriber attaches. This path is exercised in CI by the separate
  "Integration Tests (Docker)" job.

See [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md) for full platform context.
