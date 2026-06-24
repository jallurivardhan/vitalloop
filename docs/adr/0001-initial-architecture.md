# ADR 0001 — Initial Architecture: Event-Driven Microservices on GCP with ADK

**Status:** Accepted
**Date:** 2024-01-01
**Author:** VitalLoop maintainers

---

## Context

VitalLoop needs an architecture that can:

1. Ingest continuous, high-frequency vital-sign streams from patient devices.
2. Run near-real-time AI-powered triage on incoming data.
3. Serve a low-latency clinician dashboard with live updates.
4. Store clinical data in an interoperable, auditable format.
5. Scale individual components independently based on load.
6. Be developed by a small team in an open-source context with clear
   modularity boundaries.

The primary alternatives considered were:

- **Monolith**: Single Spring Boot application with in-process modules.
- **Event-driven microservices**: Separate services communicating via
  a message broker (Pub/Sub).
- **Serverless functions**: Individual Cloud Functions per event handler.

---

## Decision

We adopt **event-driven microservices** on **Google Cloud Platform** using
**Java 17 + Spring Boot 3.4.x** as the application framework, structured as a
**Maven multi-module monorepo**.

### Key architectural choices

#### 1. Event-driven over synchronous RPC

Services communicate via **Google Cloud Pub/Sub** topics, not HTTP calls.
This decouples producers from consumers, allows independent scaling, provides
natural back-pressure, and makes the system resilient to downstream failures.
Synchronous REST is used only at the external boundary (`clinician-bff`).

#### 2. FHIR R4 as the clinical system of record

Clinical data (observations, patients, care plans) is stored as **FHIR R4**
resources in the **GCP Cloud Healthcare API** FHIR store. This provides
healthcare interoperability (HL7 FHIR compliance), built-in versioning and
audit history, and a standard query API. Operational/relational data lives in
**Cloud SQL (PostgreSQL)** under the `operational` schema.

#### 3. Java 17 + Spring Boot 3.4.x

Spring Boot provides a mature, well-documented ecosystem for building
production-grade microservices. Spring Integration and Spring Cloud GCP offer
first-class Pub/Sub and Cloud SQL integration. Java 17 LTS is widely supported
by GCP runtimes. The team's existing expertise is in the JVM ecosystem.

#### 4. Google ADK + Gemini on Vertex AI for the agent pipeline

The triage pipeline requires multi-step reasoning with access to patient
context from the FHIR store. The **Google Agent Development Kit (ADK)** with
**Gemini on Vertex AI** provides a structured way to compose multi-agent
pipelines with tool use, grounding, and observability. This avoids building
a custom orchestration layer from scratch.

#### 5. Maven multi-module monorepo

A single repository with a parent POM allows shared dependency management,
consistent tooling (Spotless, Surefire), and atomic cross-service commits
during early development. Services can be extracted to separate repositories
later without restructuring the build.

---

## Consequences

### Positive

- Independent scaling: ingestion and triage services can scale horizontally
  without affecting the BFF.
- Fault isolation: a slow or failing triage pipeline does not block vital-sign
  ingestion.
- FHIR interoperability enables future integration with EHR systems.
- Clear modularity reduces cognitive load per service.
- ADK provides a testable, observable framework for the AI pipeline.

### Negative / Trade-offs

- **Operational complexity**: running five services + Pub/Sub + FHIR store
  locally requires Docker Compose; debugging distributed traces requires
  Cloud Trace / Zipkin.
- **Eventual consistency**: because communication is async, the dashboard
  may lag briefly behind the latest ingested vital signs.
- **Cold-start latency**: Cloud Run cold starts may add latency to the
  triage pipeline for bursty workloads (mitigated by minimum instance settings).
- **Monorepo overhead**: all services share a Maven reactor, which means
  the parent POM must be kept clean and modules well-isolated.

### Risks and mitigations

| Risk | Mitigation |
|---|---|
| Pub/Sub message loss | Enable dead-letter topics and at-least-once delivery |
| FHIR store becomes a bottleneck | Cache read-heavy lookups in Redis (future ADR) |
| ADK API changes | Pin ADK version; review release notes on upgrades |
| PHI leakage via logs | Structured logging policy; never log resource content |
