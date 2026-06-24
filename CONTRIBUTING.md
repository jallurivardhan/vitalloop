# Contributing to VitalLoop

Thank you for your interest in contributing! Please read this guide before
submitting issues or pull requests.

> **Before contributing**, please review [DISCLAIMER.md](DISCLAIMER.md) and
> [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

---

## Table of Contents

- [Getting Started](#getting-started)
- [Branching Strategy](#branching-strategy)
- [Commit Messages](#commit-messages)
- [Code Style](#code-style)
- [Building the Project](#building-the-project)
- [Running the Dev Environment](#running-the-dev-environment)
- [Pull Request Process](#pull-request-process)
- [Data Safety](#data-safety)

---

## Getting Started

1. Fork the repository and clone your fork.
2. Copy `.env.example` to `.env` and configure local values.
3. Start infrastructure: `make dev-up`.
4. Verify the build passes: `make build`.

---

## Branching Strategy

VitalLoop follows **trunk-based development**:

- `main` is the trunk — it must always be in a releasable state.
- Create **short-lived feature branches** off `main`:
  ```
  git checkout -b feat/your-feature-name
  ```
- Branches should be open for **no more than 2–3 days** before merging.
- **All changes require a pull request** — direct pushes to `main` are blocked.
- Delete your branch after it is merged.

---

## Commit Messages

We use **[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)**.
Every commit message must follow this format:

```
<type>(<optional scope>): <short description>

[optional body]

[optional footer(s)]
```

### Allowed types

| Type | When to use |
|---|---|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only changes |
| `style` | Formatting changes (no logic change) |
| `refactor` | Code restructuring (no feature/fix) |
| `test` | Adding or fixing tests |
| `chore` | Build tooling, CI, dependency updates |
| `perf` | Performance improvements |
| `ci` | CI/CD configuration changes |
| `revert` | Reverting a previous commit |

### Examples

```
feat(ingestion): add FHIR Observation ingest endpoint
fix(triage-agent): correct null check on missing vital sign
docs(arch): update ADR for event schema versioning
chore(deps): bump Spring Boot to 3.4.2
```

---

## Code Style

All Java code is formatted using **google-java-format** via the Spotless Maven plugin.

- **Before committing**, run:
  ```bash
  make format
  ```
- The CI pipeline runs `mvn verify`, which includes a Spotless **check** step.
  PRs with formatting violations will fail CI.
- Do not disable the Spotless check or bypass formatting with `// @formatter:off`
  blocks unless there is a documented reason in a code comment.

---

## Building the Project

```bash
# Full build: compile + unit tests + integration tests + Spotless check
make build
# Equivalent to:
mvn -B verify
```

Requires JDK 17 and Maven 3.9+. Temurin JDK is recommended.

---

## Running the Dev Environment

```bash
# Start PostgreSQL, Pub/Sub emulator, and HAPI FHIR server
make dev-up

# Tail logs
make dev-logs

# Stop everything (volumes are preserved)
make dev-down
```

See [README.md](README.md#local-development) for port mappings and prerequisites.

---

## Pull Request Process

1. Ensure `make build` passes locally before opening a PR.
2. Fill in the PR template completely.
3. PRs require **at least one approving review** from a code owner.
4. Squash merges are preferred to keep `main` history clean.
5. Link any relevant GitHub issues in the PR description.

---

## Data Safety

- **Never** commit real patient data (PHI) or production credentials.
- Use only synthetic or de-identified test data.
- See [DISCLAIMER.md](DISCLAIMER.md) and [SECURITY.md](SECURITY.md).
