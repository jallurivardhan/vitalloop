# infra — Terraform Infrastructure as Code

**Status:** Placeholder — implementation coming in a future phase.

## Overview

Terraform modules for provisioning all GCP infrastructure required by
VitalLoop: Cloud Run services, Cloud SQL, Cloud Pub/Sub topics and
subscriptions, Cloud Healthcare API FHIR stores, Vertex AI endpoints,
Secret Manager secrets, IAM bindings, and networking.

## Planned structure

```
infra/
  environments/
    dev/
    staging/
    prod/
  modules/
    cloud-run/
    cloud-sql/
    pubsub/
    fhir-store/
    vertex-ai/
    networking/
  main.tf
  variables.tf
  outputs.tf
  versions.tf
```

## Prerequisites (future)

- Terraform >= 1.7
- `gcloud` CLI authenticated
- GCP project with billing enabled

> **Note:** Never commit `.tfvars` files containing real credentials or
> production project IDs. Use Secret Manager or CI/CD variable injection.
