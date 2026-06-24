# notification-service

**Status:** Placeholder — implementation coming in Phase 2.

## Responsibility

Subscribes to `triage.result` events and delivers alerts to clinicians via
configurable channels (in-app push, email, SMS). Tracks notification state
and acknowledgements in Cloud SQL (`operational.notification_log`). Implements
alert deduplication and respects per-clinician notification preferences.

## Planned tech

- Spring Boot 3.4.x
- Spring Data JPA (Cloud SQL / PostgreSQL)
- Spring Cloud GCP Pub/Sub
- Firebase Cloud Messaging (push notifications)

## Pub/Sub topics

| Direction | Topic | Description |
|---|---|---|
| Subscribes | `triage.result` | Urgency classification to act on |

See [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md) for full context.
