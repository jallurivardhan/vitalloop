# VitalLoop — Architecture Overview

> This document describes the intended architecture of the VitalLoop platform.
> Implementation status per component is tracked in the project README.

---

## System Context

VitalLoop enables clinicians to monitor patients remotely via a web dashboard,
and patients to submit vital signs via a mobile app. An AI-powered triage
pipeline runs continuously on incoming data, escalating anomalies to the
clinical team via configurable notifications.

---

## Microservices

The platform is decomposed into five focused microservices. They communicate
exclusively over **Google Cloud Pub/Sub** topics — there are no synchronous
service-to-service HTTP calls in the hot path, which decouples services,
enables independent scaling, and provides natural back-pressure.

### Current event flow (implemented)

The first asynchronous slice of the platform is in place:

```
client ──HTTP POST /api/v1/vitals──▶ ingestion-service (:8081)
                                          │  validate + enrich (eventId, recordedAt)
                                          │  publish VitalsReadingReceived
                                          ▼
                              Pub/Sub topic: vitals-reading-received
                                          │
                                          ▼  subscription: vitals-reading-received-sub
                                   triage-agent-service (:8082)
                                          │  reconstruct reading
                                          │  TriageEngine (rule-based default; ADK gated)
                                          ▼
                                   TriageResultHandler (logs decision as JSON;
                                   alerts / notifications / FHIR come later)
```

- `ingestion-service` returns **`202 Accepted`** with the published `eventId`
  immediately after publishing — it no longer triages on the request path.
- The wire contract `VitalsReadingReceived` lives in the shared **`contracts`**
  module (plain Java + Jackson), depended on by both services — see
  [ADR 0002](adr/0002-shared-contracts-module.md).
- Under the `local` Spring profile, `triage-agent-service` creates the topic and
  subscription on the Pub/Sub emulator at startup (`PubSubAdmin`, create-if-
  absent) and starts the subscriber, so `make dev-up` gives a working end-to-end
  loop. The default build/CI never connect to Pub/Sub.

The broader target architecture (FHIR persistence, the four-agent pipeline, and
the remaining services) is described below.

### 1. `ingestion-service` (port 8081)

**Responsibility:** Accept raw vital-sign readings from patient devices and
the mobile app, validate them, and publish a reading-received event.

- **Implemented today:** validates the reading, enriches it (defaults
  `recordedAt`, generates an `eventId`), publishes a `VitalsReadingReceived`
  event to the `vitals-reading-received` topic, and returns `202 Accepted`.
- **Planned:** normalize to FHIR R4 `Observation` resources and persist to the
  FHIR store; device authentication and edge rate-limiting; owns the FHIR
  `Observation` resource type.

### 2. `patient-service`

**Responsibility:** Manage patient enrollment, demographic data, device
assignments, and care-team relationships. Exposes CRUD operations over
FHIR `Patient`, `Practitioner`, and `CareTeam` resources.

- Source of truth for patient identity and care-plan context.
- Publishes `patient.enrolled` and `patient.updated` events.
- Queries Cloud SQL (`operational` schema) for enrollment metadata.

### 3. `triage-agent-service` (port 8082)

**Responsibility:** Subscribe to reading-received events and run the triage
pipeline.

- **Implemented today:** subscribes to `vitals-reading-received-sub`,
  reconstructs the reading from the event, and runs a pluggable `TriageEngine`
  (deterministic `RuleBasedTriageEngine` by default; the ADK/Gemini
  `AdkTriageEngine` is gated behind `vitalloop.triage.engine=adk`). The decision
  is handed to a `TriageResultHandler` outbound port whose only adapter today
  logs it as structured JSON. Ports-and-adapters, same as ingestion.
- **Planned:** the full four-agent ADK pipeline and publishing `triage.result`
  events.

See [ADK Agent Pipeline](#adk-agent-pipeline) below.

### 4. `notification-service`

**Responsibility:** Subscribe to `triage.result` events and deliver
alerts to clinicians via configurable channels (in-app push, email, SMS).
Tracks notification state and acknowledgements in Cloud SQL.

- Implements alert deduplication and suppression windows.
- Respects clinician notification preferences.

### 5. `clinician-bff` (Backend for Frontend)

**Responsibility:** Serve the React clinician dashboard. Aggregates data
from other services via internal Pub/Sub queries and FHIR store reads.
Provides a WebSocket feed for real-time vital-sign updates.

- The only service with an external-facing HTTPS endpoint.
- Handles clinician authentication (Google Identity / OAuth 2.0).

---

## Data Architecture

### FHIR Store — Clinical System of Record

All clinical data (vital signs, patient demographics, observations, care plans)
is stored as **FHIR R4 resources** in the GCP Cloud Healthcare API FHIR store.

- Locally: replaced by a **HAPI FHIR R4** Docker container (port 8090).
- Services read/write FHIR resources directly via the FHIR REST API.
- Provides audit logging, versioning, and interoperability out of the box.

### Cloud SQL — Operational Data

**PostgreSQL 16** (Cloud SQL in GCP, Docker locally) stores operational,
relational data that does not belong in FHIR:

- Alert state and acknowledgement history (`operational.alerts`)
- Notification delivery receipts (`operational.notification_log`)
- Clinician preferences and on-call schedules (`operational.preferences`)
- Device registration and assignment (`operational.devices`)

Schema: `operational` (created by `docker/init.sql`).

---

## ADK Agent Pipeline

`triage-agent-service` hosts a four-agent pipeline built with the
**Google Agent Development Kit (ADK)** and **Gemini on Vertex AI**.

```
vital-signs.raw (Pub/Sub)
        │
        ▼
┌───────────────────┐
│  1. Triage Agent  │  Classifies urgency (normal / watch / critical)
└────────┬──────────┘  based on vital-sign values and patient baselines
         │
         ▼
┌───────────────────────┐
│  2. Context Agent     │  Enriches with patient history, care plan,
└────────┬──────────────┘  recent trends, and comorbidities from FHIR
         │
         ▼
┌──────────────────────────┐
│  3. Coordination Agent   │  Determines which care-team members to
└────────┬─────────────────┘  notify and at what priority
         │
         ▼
┌────────────────────────────┐
│  4. Documentation Agent   │  Generates a structured FHIR
└────────────────────────────┘  DocumentReference summarising the event

        │
        ▼
triage.result (Pub/Sub) → notification-service
FHIR DocumentReference  → FHIR store
```

Each agent is a stateless function; state is passed as structured context
between pipeline steps. The pipeline is triggered per Pub/Sub message and
runs to completion within a configurable timeout.

---

## Infrastructure (GCP)

| Component | GCP Service | Local Equivalent |
|---|---|---|
| Microservices | Cloud Run | JVM process / IDE |
| Messaging | Cloud Pub/Sub | Pub/Sub Emulator (port 8085) |
| Clinical data | Cloud Healthcare API (FHIR R4) | HAPI FHIR (port 8090) |
| Operational DB | Cloud SQL (PostgreSQL 16) | Docker postgres:16 (port 5432) |
| AI agents | Vertex AI + ADK | Vertex AI (no local emulator) |
| Secrets | Secret Manager | `.env` file |
| CI/CD | GitHub Actions → Cloud Build | GitHub Actions |
| IaC | Terraform | `infra/` (Terraform) |

---

## Security Considerations

- All inter-service communication uses service accounts with least-privilege IAM.
- The FHIR store enforces field-level access controls via IAM conditions.
- PHI must never appear in Pub/Sub message attributes (use resource references).
- `clinician-bff` is the only internet-facing surface; all other services are VPC-internal.
