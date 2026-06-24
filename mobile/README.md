# mobile — Capacitor Patient App

**Status:** Placeholder — implementation coming in a future phase.

## Overview

The patient-facing mobile application. Allows patients to submit vital-sign
readings, view their monitoring status, and receive notifications from their
care team. Targets iOS and Android via Apache Capacitor.

## Planned tech

- React 18+ (shared component library with `web/`)
- TypeScript
- Apache Capacitor
- Ionic Framework (UI components)
- Capacitor Health (Apple HealthKit / Google Health Connect integration)

## Development

> Mobile build tooling will be configured when this module is scaffolded.

Vital-sign readings submitted from the mobile app are received by
[`ingestion-service`](../services/ingestion-service/README.md).
