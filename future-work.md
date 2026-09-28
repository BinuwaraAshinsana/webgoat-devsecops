# future-work.md — What Is Still Outstanding

**Module:** IE3142 DevOps Security · **Assignment:** Building and Securing a DevSecOps Pipeline
**App:** OWASP WebGoat (Java 25 / Spring Boot / Maven)
**Written:** 28 September 2026
**Deadline:** 1 October 2026, CourseWeb

> Completed work is deliberately not repeated here. Already done: GitHub repo + PR
> workflow + member branches, architecture diagram v1, base README, the
> `docs/{evidence,diagrams,report}/` structure, `.env.example`, README test and
> secrets sections, `graphify-out/` ignored, JDK 25 installed. Everything below is
> what remains.

---

## 0. Read this before planning anything

**There are 3 days left, not 16.** `team.md` and `step.md` were written on 15 Sep
against a 16-day runway and schedule everything from Sep 15 to Sep 30. That
calendar is dead. As of today:

| Stream | Planned finish | Actual state |
|--------|----------------|--------------|
| M1 — containers | Sep 19 | Compose is single-service; Dockerfile still needs a pre-built jar |
| M2 — threat model | Sep 24 | Not started (branch is 0 commits ahead of `main`) |
| M3 — exploit & fix | Sep 25 | Not started. **No SAST baseline captured** |
| M4 — CI/CD + secrets | Sep 27 | Not started. `.github/` does not exist |

Three of the four work streams are unstarted, and they carry **30 of the 50
marks**. Sequence the remaining time by marks-per-hour, not by the original plan:

1. **M3 secure coding** — 11 marks, and the SAST baseline must be taken *before*
   any fix is committed. This is the one thing that becomes impossible if delayed.
2. **M4 CI/CD** — 10 marks, and the failing-gate run needs the fixes merged first.
3. **M2 threat model** — 9 marks, writing-only, can run in parallel.
4. Report assembly and the contribution statement — 3 marks, but the submission is
   rejected without them.

The viva is a further 25 marks and is marked **individually**. Artifacts you
cannot explain earn nothing.

---

## 1. Blockers — clear these first

These gate other people's work. Nothing else in Section 3 or 4 can finish until
they are done.

- [ ] **B1. Decide the two-component split.** The assignment requires two separate
      components that communicate. Compose currently runs one container exposing
      both 8080 and 9090, which does not demonstrate this.

      ⚠️ The approach in `step.md` Step 1.2 option (a) — "run the same image twice
      with different Spring profiles, no code changes" — **does not work**:

      - `src/main/java/org/owasp/webgoat/server/StartWebGoat.java:19-35` boots
        *both* WebWolf and WebGoat as child contexts in one JVM. There is no
        profile switch, so running the image twice gives two containers each
        running both apps.
      - Both profiles point at the same HSQLDB **file** database
        (`application-webgoat.properties:19`, `application-webwolf.properties:23`).
        HSQLDB file mode is single-process — two JVMs on a shared volume deadlock
        on the lock file.

      **Recommended:** run **HSQLDB in server mode as its own container**. Change
      the two JDBC URLs to `jdbc:hsqldb:hsql://db/webgoat`, add a `db` service on
      an internal network. `flyway-database-hsqldb` and `HSQLDialect` stay as they
      are, and no migration is rewritten. This gives a genuine app ↔ database
      network trust boundary — the PDF's own example of a valid pairing.

      Do **not** attempt the PostgreSQL swap at this point: it is a larger job than
      `team.md` suggests, and
      `webgoat.database.connection.string=jdbc:hsqldb:mem:{USER}`
      (`application-webgoat.properties:57`) must stay HSQLDB, because the SQL
      injection lessons depend on it.

- [ ] **B2. Make the Docker image build itself (multi-stage).** `Dockerfile:17` is
      still `COPY --chown=webgoat target/webgoat-*.jar`, so it needs a manual
      `mvn package` first. There is no jar in `target/` today, so a clean clone
      cannot build at all.

      - Stage 1: Maven + JDK 25, copy source, `./mvnw -DskipTests package`
      - Stage 2: `eclipse-temurin:25-jdk-noble` (keep the JDK — lessons compile
        Java at runtime), copy the jar, keep `USER webgoat`, `ENTRYPOINT`,
        `HEALTHCHECK` unchanged
      - **Also fix `.dockerignore`.** It is currently `**` + `!/target` +
        `!/config/desktop`, which excludes `src/`, `pom.xml` and `mvnw` from the
        build context — a multi-stage build cannot see the sources.
      - Verify: `docker build -t webgoat:local .` must succeed with no `target/`.

- [ ] **B3. Rewrite `docker-compose.yml`.** Two services on an internal network,
      only intended ports published, `${VARIABLE}` substitution wired to the
      existing `.env.example` / `.env`. Prove it: `docker compose up` on a clean
      clone reaches <http://localhost:8080/WebGoat> and
      <http://localhost:9090/WebWolf> with **one command**.

- [ ] **B4. Redraw the architecture diagram** for the new topology. The current one
      (`docs/diagrams/architecture.png`) shows the single-container reality and one
      boundary. It needs three distinct trust boundaries: **public internet |
      internal container network | database**. Keep the `.drawio` source in sync.
      Tell M2 the moment it lands — they are blocked on it.

---

## 2. Repo hygiene — quick, do them today

- [ ] **Add `.pre-commit-config.yaml`.** It does not exist. Nothing currently
      enforces commit format, YAML validity or Java formatting.
      Everyone then runs `pip install pre-commit && pre-commit install` once.
- [ ] **Fix the Spotless failure.** `./mvnw verify` is **red right now**: the build
      formats `**/*.md`, and `step.md` and `team.md` both fail `spotless:check`.
      They are untracked so CI has not seen them yet — but the moment anyone
      commits them, the pipeline goes red. Either run
      `./mvnw spotless:apply -DspotlessFiles='.*step\.md|.*team\.md'` or add a
      Spotless exclude for planning docs. Same applies to this file.
- [ ] **Adopt Conventional Commits from here on.** `feat:`, `fix:`, `docs:`,
      `ci:`, `chore:`. Required by the Definition of Done.
- [ ] **Add a `.mailmap`** so `git shortlog` shows one identity per person.
- [ ] **Decide whether `step.md`, `team.md` and the assignment PDF get committed.**
      Currently untracked. The planning docs are reasonable to commit; the
      assignment brief probably should not be.
- [ ] **Restore the licence before submission.** `LICENSE.txt` and `COPYRIGHT.txt`
      were removed on 2026-09-28 and the README licence section with them. WebGoat
      is GPL-2.0-or-later and this repo is a public derivative work, so they must
      come back before you submit or publish:

      ```bash
      git checkout 9225f45 -- LICENSE.txt COPYRIGHT.txt
      ```

      Then re-add to the README, with the version stated correctly:
      > This project is based on OWASP WebGoat
      > (github.com/WebGoat/WebGoat), licensed under **GPL-2.0-or-later**.
      > Modifications by [group] are described in Section X.

      SPDX headers in the 411 Java files are untouched and must stay that way —
      Spotless enforces them via `config/license-headers/java`.

---

## 3. M3 — Secure Coding (11 marks) 🔴 **start here**

### 3.1 Capture the SAST baseline — before any fix is committed

Once a fix lands without a baseline, the before/after comparison is gone
permanently. This is the single most time-critical task in the project.

```bash
pip install semgrep
semgrep --config p/java --config p/owasp-top-ten --sarif -o docs/evidence/sast-baseline.sarif
semgrep --config p/java --config p/owasp-top-ten --json  -o docs/evidence/sast-baseline.json
git add docs/evidence/sast-baseline.*
git commit -m "docs: add semgrep baseline scan before any fixes"
git tag baseline-sast
git push --tags
```

- [ ] Baseline scan captured, committed and tagged
- [ ] Total finding count recorded, plus the per-file count for each file to be fixed

> Bandit is Python-only and does not apply to this Java stack. The PDF lists it
> only as an example.

### 3.2 The four exploit-and-fix demos

At least four vulnerabilities, each with **all four** evidence stages. A fix with
no demonstrated exploit counts as basic secure-coding evidence only.

| # | Vulnerability | Module | Fix technique | Done |
|---|---------------|--------|---------------|------|
| V1 | SQL Injection | `lessons/sqlinjection` | `PreparedStatement` / parameterised query | [ ] |
| V2 | Stored / reflected XSS | `lessons/xss` | Output encoding + CSP header | [ ] |
| V3 | IDOR / missing access control | `lessons/idor`, `lessons/missingac` | Server-side authorisation check | [ ] |
| V4 | Path traversal / XXE / JWT bypass | `lessons/pathtraversal`, `xxe`, `jwt` | Canonical path check / disable external entities / verify signature and algorithm | [ ] |

`deserialization`, `ssrf`, `csrf`, `openredirect` and `insecurelogin` are also
available in the repo if you want to swap.

**Per vulnerability, all four stages:**

- [ ] 1. Exploit works — payload + response screenshot against unmodified code
      → `docs/evidence/vN-name-before.png`
- [ ] 2. Fix — `git diff` plus 2–3 sentences on *why* it works
      → `docs/evidence/vN-name-diff.txt`
- [ ] 3. Exploit blocked — the **exact same** payload, screenshot of the failure
      → `docs/evidence/vN-name-after.png`
- [ ] 4. SAST delta — finding count for that file, before and after
      → `docs/evidence/vN-name-sast.txt`

### 3.3 Final scan and collation

- [ ] `semgrep --config p/java --config p/owasp-top-ten --json -o docs/evidence/sast-after.json`
- [ ] Summary table: per vulnerability — findings before, findings after, exploit blocked yes/no
- [ ] Tell M4 the moment all four fixes are merged — the failing-gate demo depends on it

---

## 4. M4 — CI/CD Pipeline and Secrets (10 marks)

`.github/` **does not exist in this repo at all** — no workflows, no
`build.yml`, no dependabot. You are starting from an empty directory. The
warnings in `team.md` §5.2 about not touching the existing `build.yml` are moot;
there is nothing to avoid.

### 4.1 Base pipeline

- [ ] Create `.github/workflows/devsecops.yml`, triggered `on: [push, pull_request]`
- [ ] `actions/setup-java@v4`, `distribution: temurin`, `java-version: '25'`
      (`pom.xml:77` sets `<java.version>25</java.version>`), `cache: maven`
- [ ] `./mvnw -B verify` — one green run on a clean commit **before** adding any gate

> `verify` runs the Playwright integration tests (Failsafe is bound to
> `integration-test` and `verify`) and `spotless:check`. Expect a slow first run,
> and fix the Markdown formatting violations in Section 2 or this job stays red.

### 4.2 The four mandatory gates

| Gate | Tool | Notes | Done |
|------|------|-------|------|
| SAST | Semgrep | `p/java` + `p/owasp-top-ten` — same config M3 uses, so the numbers match | [ ] |
| Dependency / SCA | OWASP Dependency-Check Maven plugin, or `trivy fs .` | `config/dependency-check/project-suppression.xml` already exists | [ ] |
| Secrets | Gitleaks | Scan the working tree **and** the git history | [ ] |
| Container image | Trivy `image` mode | Must run against the image the pipeline **builds** — depends on B2 | [ ] |

### 4.3 Handle the deliberately-vulnerable-app problem

WebGoat is intentionally insecure, so all four scanners return hundreds of
findings and a naive "fail on HIGH" gate red-builds every commit forever. Pick
one approach and **write the reasoning into the report** — that reasoning is
itself worth marks:

- [ ] (a) Baseline/allowlist the known intentional lesson findings, fail only on new ones, **or**
- [ ] (b) Scope the failing gate to files changed in the PR, **or**
- [ ] (c) Fail only on findings **outside** lesson code — `server/`, `container/`, `webwolf/`

(c) is the easiest to defend in a viva.

### 4.4 Make a gate genuinely fail 🔴 required

A warning is not enough — the PDF requires a captured run where a gate **blocks**
the build.

- [ ] Throwaway branch `demo/failing-gate`
- [ ] Break something deliberately: commit a fake AWS key (Gitleaks) or revert
      M3's SQLi fix (Semgrep)
- [ ] Push, watch it go red
- [ ] Screenshot the red run **and** the tool output → `docs/evidence/`
- [ ] Save the run URL for the report
- [ ] Delete the branch, never merge it

### 4.5 Secrets management

- [ ] Audit for hardcoded credentials:
      `grep -rniE "password|secret|api[_-]?key|token" --include="*.properties" --include="*.yml" .`
- [ ] Move anything real into GitHub Actions encrypted secrets
      (Settings → Secrets and variables → Actions)
- [ ] Wire `docker-compose.yml` to `${VARIABLE}` from `.env` (overlaps B3;
      `.env.example` already exists and is already gitignored correctly)
- [ ] Document both provisioning paths for the report: how a secret reaches
      (a) the pipeline, and (b) the running container

### 4.6 Bonus — only once the four gates are green

- [ ] HashiCorp Vault dev container in compose, app pulls one secret at runtime
      (PDF: "marked more favourably")
- [ ] OWASP ZAP `zap-baseline` DAST job against the running stack (extra credit)

---

## 5. M2 — Threat Modelling and Risk (9 marks)

Blocked on **B4** (the redrawn diagram). Everything here is writing, so it can run
in parallel with M3 and M4 once the diagram exists.

- [ ] **≥4 STRIDE threats**, each naming a real WebGoat file or feature. Generic
      template threats are explicitly marked down.

| STRIDE | Candidate threat | Where it lives |
|--------|------------------|----------------|
| **S**poofing | Session/cookie hijacking; weak default credentials | `lessons/hijacksession/`, `lessons/spoofcookie/` |
| **T**ampering | Browser-only validation; JWT signature bypass | `lessons/clientsidefiltering/`, `lessons/jwt/` |
| **R**epudiation | No security logging or admin audit trail | `lessons/logging/` |
| **I**nfo disclosure | Verbose Spring error pages; exposed `/actuator`; path traversal | `lessons/pathtraversal/`, `application-webgoat.properties:79` exposes `env, health, configprops` |
| **D**oS | Unbounded upload; no login rate limiting | lesson upload endpoints |
| **E**levation of privilege | Missing access control, IDOR, auth bypass | `lessons/missingac/`, `lessons/idor/`, `lessons/authbypass/` |

- [ ] Pick 4–6, choosing ones that **match the vulnerabilities M3 is actually
      fixing** — the table and the evidence then line up
- [ ] 3×3 likelihood × impact matrix, with **1–2 sentences of justification each**.
      The marks are in the justification, not the grid.
- [ ] **Threat-to-control table** giving the *exact* location for each control —
      `src/main/java/.../SqlInjectionLesson5a.java:NN`, or a named step in
      `.github/workflows/devsecops.yml`. Build this sitting with M3 and M4.
- [ ] **Industry Trends & Case Study (~300 words).** ⚠️ 5 marks, and it appears
      only in the rubric, never in the task list. Pick one: **SolarWinds**
      (build-pipeline compromise → our image/build scanning), **Log4Shell**
      (transitive dependency risk → our SCA gate; WebGoat even ships a
      `vulnerablecomponents` lesson), or **Codecov** (CI secrets exfiltration →
      our secrets handling).

---

## 6. M1 — Remaining Architecture Work

Beyond blockers B1–B4:

- [ ] Update the README once compose actually works with one command
- [ ] Restore the licence statement (Section 2)
- [ ] Report: table of contents, executive summary, system overview (tech stack,
      containerisation approach)
- [ ] IEEE reference list, including OWASP WebGoat itself — the entire codebase is
      external code, so citing it is not optional
- [ ] Act as report editor: assemble all sections, trim to **1800–2500 words
      excluding appendices**

---

## 7. Everyone — Shared Deliverables

- [ ] **Signed Ethical Clearance Form** ⚠️ Work carried out without a submitted
      form may not be accepted. Nothing for this exists in the repo.
- [ ] **Individual Contribution Statement + AI disclosure**, signed by all four.
      Must state which tool, for what purpose, for which parts. Undisclosed use is
      an academic integrity violation.
- [ ] **Reflection (~150 words)** — technically specific. "Replace the embedded
      HSQLDB with a networked database to demonstrate a real trust boundary" scores;
      "manage time better" does not.
- [ ] **Grant the module team repository access** before submitting
- [ ] **Mock viva** — each person presents *someone else's* section. Fastest way to
      find the gaps while there is still time.

### Report word budget (~2,450 words)

| Section | Words | Owner |
|---------|------:|-------|
| Executive summary + system overview | 250 | M1 |
| Threat model & risk assessment | 450 | M2 |
| Secure coding walkthrough | 550 | M3 |
| CI/CD pipeline & gates | 450 | M4 |
| Secrets management | 200 | M4 |
| Industry trends / case study | 300 | M2 |
| Reflection | 150 | M2 |
| Contribution statement | 100 | All |

---

## 8. Dependency Order

```text
B1 database decision
      └─> B2 self-building image ──> B3 compose (one command) ──> B4 diagram ──> M2 threat model
                                              │                                      └─> threat-to-control table
                                              └─> M4 Trivy image gate

M3 SAST baseline  ──(must precede every fix)──> 4 exploits ──> 4 fixes ──> after-scan diff
                                                                   └─> M4 failing-gate demo
```

**Independent of everything — start now:** M3's SAST baseline, M2's industry case
study, the Ethical Clearance Form, `.pre-commit-config.yaml`.

---

## 9. Definition of Done

**Technical**

- [ ] `docker compose up` starts both components with one command on a clean machine
- [ ] Architecture diagram with components, data flows and trust boundaries
- [ ] ≥4 STRIDE threats, rated, justified, each mapped to a control and its exact location
- [ ] 4 × (exploit works → fix → exploit blocked → SAST before/after), with screenshots
- [ ] Pipeline runs on every push; SAST + dependency + secrets + container gates all present
- [ ] A captured pipeline run where a gate **fails** the build
- [ ] Zero hardcoded credentials; secrets via GitHub Actions encrypted secrets
- [ ] Commit history shows all four members, with Conventional Commit messages

**Licensing and originality**

- [ ] `LICENSE.txt` / `COPYRIGHT.txt` restored; SPDX headers intact; no conflicting licence
- [ ] README and report state the project derives from OWASP WebGoat under GPL-2.0-or-later
- [ ] OWASP WebGoat cited in the IEEE reference list
- [ ] All report text, diagrams and screenshots are the team's own, from the team's own runs

**Submission package**

- [ ] Signed Ethical Clearance Form
- [ ] Repository link, module team granted access, README complete
- [ ] Report PDF: ToC, 1800–2500 words, IEEE references, reflection, signed
      contribution statement with AI disclosure
- [ ] Uploaded to courseweb.sliit.lk before **1 October 2026**
