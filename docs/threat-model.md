# Threat Model and Risk Assessment

STRIDE threat model for the WebGoat DevSecOps deployment, with a likelihood ×
impact risk assessment and a threat-to-control mapping. Each threat names a real
component of this application, and each control points at the exact file or
pipeline step that implements it.

## 1. Scope and architecture

The system under assessment is the containerised WebGoat stack in this
repository (see `docs/diagrams/architecture.png`):

- A single Docker container runs both **WebGoat** (port 8080) and **WebWolf**
  (port 9090) in one JVM (`server/StartWebGoat.java`).
- Persistence is an **embedded HSQLDB** database inside the container.
- `docker-compose.yml` publishes both ports on `127.0.0.1` only, so the
  application is reachable from the host loopback, not external interfaces.

**Trust boundaries.** Two boundaries are relevant:

1. **Host / user ↔ container** — HTTP requests cross from the browser (untrusted
   input) into the application. This is where all of Section 3's threats act.
2. **Application ↔ database** — the app trusts the query layer. Because HSQLDB is
   embedded in the same process, there is no network boundary here today; this is
   itself a weakness (an external database would give a stronger, observable
   boundary) and is noted as residual risk R-DB in Section 5.

Because WebGoat is deliberately vulnerable, the threats below are the *realistic
attacks a real deployment of this code would face*, tied to the specific lesson
code that exhibits them — not generic textbook threats.

## 2. STRIDE threats

Six threats, one per STRIDE category, each anchored to a real file or feature.

| ID |           STRIDE           |                                                                                                                            Threat                                                                                                                             |                                                                                                                   Where it lives in this app                                                                                                                   |
|----|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| T1 | **S**poofing               | Well-known default credentials let an attacker log in as another user, including the admin.                                                                                                                                                                   | `container/users/DefaultUserInitializer.java:25-27` seeds `webgoat-admin` and `webgoat-user`, both with the password `webgoat`; the admin is given `ROLE_ADMIN`.                                                                                               |
| T2 | **T**ampering              | SQL injection through a lesson endpoint changes the structure of a query and returns unauthorised rows.                                                                                                                                                       | `lessons/sqlinjection/introduction/SqlInjectionLesson5a.java` and the wider `sqlinjection` module originally built queries by string concatenation.                                                                                                            |
| T3 | **R**epudiation            | Authentication and admin actions are not recorded in a tamper-evident audit trail, so a malicious or compromised user can deny their actions.                                                                                                                 | The `lessons/logging` module demonstrates missing/insufficient security logging; the container has no structured audit log for auth or admin events.                                                                                                           |
| T4 | **I**nformation disclosure | (a) Path traversal reads files outside the intended directory; (b) verbose error pages and an exposed `/actuator/env` endpoint leak configuration and stack traces.                                                                                           | (a) `lessons/pathtraversal/ProfileUploadRetrieval.java` reached `path-traversal-secret.jpg` in a parent directory. (b) `application-webgoat.properties:1` sets `include-stacktrace=always` and `:82` sets `management.endpoints.web.exposure.include=env,...`. |
| T5 | **D**enial of service      | Expensive or unbounded operations exhaust resources: lesson endpoints compile Java at runtime, file uploads are accepted, and there is no rate limiting on login.                                                                                             | `lessons/sqlinjection/mitigation` compiles submitted Java; `webwolf` file upload endpoints; no throttling on `/login`.                                                                                                                                         |
| T6 | **E**levation of privilege | (a) An open redirect is used to phish a victim and harvest credentials, leading to account takeover; (b) weak MD5 hashing lets a stolen hash be reversed to the plaintext password; (c) missing access control / IDOR lets a user act on another user's data. | (a) `lessons/openredirect/OpenRedirectRealRedirect.java`; (b) `lessons/cryptography/HashingAssignment.java` hashed secrets with MD5; (c) `lessons/missingac`, `lessons/idor`.                                                                                  |

## 3. Risk assessment

Likelihood and impact are each rated Low / Medium / High on a 3×3 matrix. The
justification, not the grid position, is where the reasoning lives.

| ID | Likelihood | Impact |    Risk    |                                                                                                                                 Justification                                                                                                                                 |
|----|------------|--------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| T1 | High       | High   | **High**   | The credentials are hard-coded and published in the source, and the seeded admin holds `ROLE_ADMIN`; anyone who reads the repo can log in as admin with no effort.                                                                                                            |
| T2 | High       | High   | **High**   | The injection point takes unauthenticated request input and the payload `' OR '1'='1` returned every row of `user_data` including card numbers; injection is trivially automated.                                                                                             |
| T3 | Medium     | Medium | **Medium** | Exploiting repudiation needs an attacker to already have access, so likelihood is moderate; impact is the loss of accountability and forensic evidence rather than direct data loss.                                                                                          |
| T4 | High       | Medium | **High**   | Both sub-threats are reachable by an unauthenticated request: the traversal recovered an out-of-directory secret, and `/actuator/env` plus full stack traces expose configuration that aids further attacks. Impact is disclosure, not integrity loss, so it is rated Medium. |
| T5 | Medium     | Medium | **Medium** | The runtime-compilation and upload endpoints are reachable, but they need a session and the loopback binding limits exposure; impact is degraded availability of a single-container demo rather than a fleet.                                                                 |
| T6 | Medium     | High   | **High**   | Each vector needs a small amount of attacker effort (crafting a redirect, cracking a hash, or guessing an id), but success yields another user's or the admin's privileges — a full confidentiality and integrity breach.                                                     |

## 4. Threat-to-control mapping

Each threat is mapped to the specific control that mitigates it and the exact
location of that control. Controls in **bold** were implemented in this project
(the Section 2.3 secure-coding fixes and the Section 2.4 pipeline gates);
italic entries are residual or recommended controls, carried into Section 5.

| ID |                                                                                                                              Control                                                                                                                               |                                                                                       Location                                                                                       |
|----|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| T1 | **Secrets are never hard-coded**: the Gitleaks gate fails the build on any committed credential, and real secrets are injected from the environment, not the source. *Residual:* WebGoat's teaching default accounts remain by design.                             | `.github/workflows/devsecops.yml` → "Secrets - Gitleaks (hard gate)"; `docker-compose.yml` `${VAR}` injection; `.env` (gitignored).                                                  |
| T2 | **Parameterised query** binds the account name instead of concatenating it, so input can no longer change the query structure. Detected by the **SAST gate**.                                                                                                      | `lessons/sqlinjection/introduction/SqlInjectionLesson5a.java:45` (fix, CWE-89); `devsecops.yml` → "SAST - Semgrep".                                                                  |
| T3 | *Recommended:* add structured security logging and an append-only audit trail for authentication and admin actions. Not yet implemented.                                                                                                                           | Residual risk R-LOG (Section 5).                                                                                                                                                     |
| T4 | **Path canonicalisation + directory containment** stops the traversal (a); the **SAST gate** reports the dangerous actuator exposure (b). *Recommended:* restrict `exposure.include` to `health` and disable stack traces in production.                           | (a) `lessons/pathtraversal/ProfileUploadRetrieval.java:101` (fix, CWE-22); (b) Semgrep flags `application-webgoat.properties:82` (`spring-actuator-dangerous-endpoints-enabled`).    |
| T5 | **Upload size limits** are enforced (1 MB) for WebWolf; the **container image scan** surfaces vulnerable base-image packages that DoS could exploit. *Recommended:* rate-limit `/login` and cap runtime compilation.                                               | `application-webwolf.properties` (`spring.servlet.multipart.max-file-size=1MB`); `devsecops.yml` → "Container image - Trivy".                                                        |
| T6 | **Redirect allowlist** blocks off-site redirects (a); **salted bcrypt** replaces MD5 so stolen hashes cannot be reversed (b). Both detected by the **SAST gate**. *Recommended:* server-side authorisation checks for the IDOR/missing-access-control vectors (c). | (a) `lessons/openredirect/OpenRedirectRealRedirect.java:27` (fix, CWE-601); (b) `lessons/cryptography/HashingAssignment.java:40` (fix, CWE-328); `devsecops.yml` → "SAST - Semgrep". |

## 5. Residual risks and recommendations

- **R-DB** — the database is embedded in the application process, so there is no
  observable app↔database trust boundary. Recommendation: externalise it (e.g. a
  networked HSQLDB or PostgreSQL container) to create a real, monitorable
  boundary.
- **R-LOG** (T3) — no security audit logging. Recommendation: structured,
  append-only logging of authentication and admin actions.
- **R-CREDS** (T1) — WebGoat's default demo accounts remain by design.
  Recommendation: for any non-teaching deployment, remove the seeded accounts and
  require first-run credential setup.
- **R-CONFIG** (T4b) — verbose stack traces and the `/actuator/env` endpoint are
  enabled. Recommendation: disable stack traces and expose only
  `/actuator/health` in production.
- **R-RATE** (T5) — no rate limiting on authentication. Recommendation: add
  login throttling and lockout.

These residual items are the honest boundary of the current work: the four
implemented fixes and four pipeline gates close the highest-risk threats
(T2, T4a, T6a, T6b) with verifiable evidence, while the items above are the next
controls a production hardening pass would add.
