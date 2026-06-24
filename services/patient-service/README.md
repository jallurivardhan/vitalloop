# patient-service

**Status:** Placeholder — implementation coming in Phase 2.

## Responsibility

Manages patient enrollment, demographic data, device assignments, and
care-team relationships. Exposes CRUD operations over FHIR `Patient`,
`Practitioner`, and `CareTeam` resources, and maintains enrollment metadata
in Cloud SQL (`operational` schema).

## Planned tech

- Spring Boot 3.4.x
- Spring Data JPA (Cloud SQL / PostgreSQL)
- Spring Cloud GCP Pub/Sub
- HAPI FHIR Client (R4)

## Pub/Sub topics

| Direction | Topic | Description |
|---|---|---|
| Publishes | `patient.enrolled` | New patient enrolled |
| Publishes | `patient.updated` | Patient record updated |

See [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md) for full context.
