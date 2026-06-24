# VitalLoop

[![CI](https://github.com/your-org/vitalloop/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/vitalloop/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Status](https://img.shields.io/badge/status-work%20in%20progress-yellow.svg)](#status)

> **⚠️ DISCLAIMER — NOT FOR CLINICAL USE.**
> This is an educational/reference implementation built with synthetic and de-identified data.
> It is **not** a medical device and must **never** be used in a real clinical environment.
> See [DISCLAIMER.md](DISCLAIMER.md) for full terms.

---

## What is VitalLoop?

VitalLoop is an open-source **remote patient monitoring and clinical decision-support platform** designed as a learning reference for building modern, event-driven healthcare systems on the cloud. It demonstrates how event-driven Java/Spring Boot microservices, a FHIR-based clinical data layer, and a multi-agent AI pipeline (Google Agent Development Kit + Gemini on Vertex AI) can be composed into a coherent platform — while following best practices for security, interoperability, and observability.

---

## Architecture Summary

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Clients                                                                 │
│  React Clinician Dashboard (web/)   ·   Capacitor Patient App (mobile/) │
└────────────────────┬─────────────────────────────────┬───────────────────┘
                     │ REST / WebSocket                 │ REST
                     ▼                                  ▼
           ┌──────────────────┐              ┌─────────────────────┐
           │  clinician-bff   │              │  ingestion-service  │
           └────────┬─────────┘              └──────────┬──────────┘
                    │                                   │
                    │           Google Pub/Sub           │
                    └──────────────────────────────────►│
                                                        ▼
                              ┌──────────────────────────────────────┐
                              │  patient-service  │  triage-agent-   │
                              │                  │    service        │
                              └──────────────────┴──────────────────┘
                                       │                  │
                         FHIR R4 Store │        ADK Agents│
                    (Cloud Healthcare) │    Triage → Context → Coord
                                       │             → Documentation
                              Cloud SQL (operational schema)
```

Five microservices communicate exclusively over **Pub/Sub topics** (no synchronous service-to-service calls in the hot path). FHIR R4 is the system of record for all clinical data; Cloud SQL (PostgreSQL) holds operational/relational state.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.4.x |
| Messaging | Google Cloud Pub/Sub |
| Clinical data | FHIR R4 — GCP Cloud Healthcare API (local: HAPI FHIR) |
| Operational data | Cloud SQL for PostgreSQL 16 (local: Docker postgres:16) |
| AI agents | Google Agent Development Kit (ADK) + Gemini on Vertex AI |
| Infrastructure | Google Cloud Platform, Terraform |
| CI/CD | GitHub Actions |
| Build | Maven (multi-module monorepo) |
| Code style | google-java-format via Spotless |

---

## Status

> **Work in progress.** Repository scaffolding and local dev environment are complete. Service implementations will be added in subsequent phases.

---

## Local Development

### Prerequisites

- Docker Desktop ≥ 4.x with Compose v2
- JDK 17 (Temurin recommended)
- Maven 3.9+ (or use `./mvnw`)

### Setup

```bash
# 1. Clone the repo
git clone https://github.com/your-org/vitalloop.git
cd vitalloop

# 2. Create your local environment file
cp .env.example .env
# Edit .env — at minimum set a POSTGRES_PASSWORD

# 3. Start infrastructure dependencies
make dev-up

# 4. Verify everything is healthy
docker compose ps
```

### Service Ports (local)

| Service | Host Port | Notes |
|---|---|---|
| PostgreSQL 16 | `5432` | DB: `vitalloop`, schema: `operational` |
| Pub/Sub Emulator | `8085` | Project ID: `vitalloop-local` |
| HAPI FHIR R4 | `8090` | UI: `http://localhost:8090/fhir` |
| Spring services (future) | `808x` | Added per phase |

### Common Commands

```bash
make dev-up      # start infrastructure
make dev-down    # stop infrastructure
make dev-logs    # tail all container logs
make build       # compile + test + lint check (mvn verify)
make format      # auto-format Java code (mvn spotless:apply)
make clean       # remove build artefacts
```

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Please read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before participating.

## Security

See [SECURITY.md](SECURITY.md) for vulnerability disclosure policy.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
