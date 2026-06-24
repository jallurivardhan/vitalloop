# Security Policy

## Supported Versions

VitalLoop is currently a pre-release educational project. There are no
production deployments. Security fixes will be applied to the `main` branch.

| Version | Supported |
|---|---|
| `main` (HEAD) | Yes |
| Older branches | No |

## Reporting a Vulnerability

**Please do not open a public GitHub issue for security vulnerabilities.**

Report vulnerabilities privately by emailing **security@vitalloop.example**
(replace with your real contact address) with:

1. A description of the vulnerability and affected component(s).
2. Steps to reproduce or a proof-of-concept (no live patient data please).
3. Potential impact assessment.
4. Any suggested mitigations (optional).

You will receive an acknowledgement within **72 hours** and a status update
within **7 days**.

## Scope

Given that this is an **educational** project:

- Vulnerabilities in the scaffolding, CI configuration, or infrastructure-as-code
  are in scope.
- Vulnerabilities in third-party dependencies (report upstream first, then here).
- Social engineering, phishing, or attacks on contributors are out of scope.

## Important Reminders

- **Never** commit real PHI, production credentials, or API keys.
- Use `.env` for local secrets — it is excluded by `.gitignore`.
- Rotate any credentials accidentally committed immediately and contact
  the security team.

See [DISCLAIMER.md](DISCLAIMER.md) for the full non-clinical use disclaimer.
