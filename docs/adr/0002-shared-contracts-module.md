# ADR 0002 — Shared `contracts` module for inter-service events

**Status:** Accepted
**Date:** 2026-06-16
**Author:** VitalLoop maintainers
**Supersedes / relates to:** [ADR 0001](0001-initial-architecture.md)

---

## Context

Moving VitalLoop to an event-driven flow means services exchange messages over
Pub/Sub instead of calling each other synchronously. The first event,
`vitals.reading.received`, is **published** by `ingestion-service` and
**consumed** by `triage-agent-service`. Both sides must agree on the exact
shape of the payload (field names, types, JSON encoding).

We need a way to define that payload once and share it, while we are still a
monorepo in an early, fast-moving phase.

Options considered:

1. **Duplicate the DTO** in each service. Zero coupling, but the two copies
   drift apart silently; a renamed or retyped field becomes a runtime
   deserialization bug that no compiler catches.
2. **Shared `contracts` module** (chosen). A single, framework-free Maven
   module holds the wire-contract records; publisher and consumer both depend
   on it.
3. **Schema registry / versioned published artifact** (e.g. Protobuf/Avro in a
   registry, or a separately versioned `contracts` artifact). Strongest
   guarantees and the right answer across repositories/teams, but heavyweight
   for a single monorepo at this stage.

---

## Decision

Introduce a `contracts` Maven module containing the inter-service event
records as **plain Java + Jackson** (no Spring, no business logic). The first
contract is `VitalsReadingReceived`. Both `ingestion-service` and
`triage-agent-service` depend on `contracts`.

The module is deliberately tiny and dependency-light (only `jackson-databind`)
so it stays safe for every service to depend on.

---

## Consequences

### Positive

- **Compile-time safety against schema drift.** A change to a contract field
  breaks compilation in every producer and consumer that uses it, in the same
  build — drift cannot slip through silently.
- Single, obvious place to read "what is on the wire".
- No framework leakage: the contract can be reused by any JVM consumer.

### Negative / Trade-off

- **Mild coupling.** Publisher and consumer now share a build-time artifact, so
  a breaking contract change is a coordinated change across modules. In a
  monorepo this is acceptable (one atomic commit, one CI run) and is a fair
  trade for eliminating silent drift.
- It is *not* independently versioned, so it does not model the "old consumers
  must keep working while the contract evolves" problem.

### Future direction

In a polyrepo or multi-team setting, this module should graduate to a
**versioned, published artifact** (with explicit backward-compatibility rules)
or a **schema registry** (Protobuf/Avro with compatibility checks enforced in
CI). The current module is structured so that migration is mechanical: it has
no dependencies on the rest of the codebase.
