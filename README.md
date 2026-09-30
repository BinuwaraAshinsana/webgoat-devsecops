# WebGoat DevSecOps Security Project

Secure DevSecOps implementation using OWASP WebGoat.

## Overview

This project demonstrates how to build, containerize, test, and secure a DevSecOps pipeline around OWASP WebGoat, an intentionally vulnerable web application used for secure coding practice and security testing.

## Team

- M1 — Architecture & Containerisation
- M2 — Threat Modelling & Risk Assessment
- M3 — Secure Coding / Exploit-and-Fix
- M4 — CI/CD Pipeline & Secrets Management

## Architecture

The application runs inside a Docker container using Java/Spring Boot.

- WebGoat — Port `8080`
- WebWolf — Port `9090`
- Embedded HSQLDB database
- Docker Desktop used for containerisation

The architecture diagram is available at:

`docs/diagrams/architecture.png`

## Prerequisites

Before running the project, install:

- Git
- Docker Desktop
- Docker Compose v2
- Java JDK 25

Verify the installations:

```bash
git --version
docker --version
docker compose version
java -version
```

Java should report version **25**.

Also verify that the Maven Wrapper is using Java 25:

```powershell
.\mvnw.cmd -version
```

The output should contain something similar to:

```text
Java version: 25.x
```

### JAVA_HOME

On Windows, `JAVA_HOME` should point to the installed JDK 25 directory.

Example:

```text
JAVA_HOME=C:\Program Files\Java\jdk-25.0.4
```

The Windows `Path` environment variable should contain:

```text
%JAVA_HOME%\bin
```

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/BinuwaraAshinsana/webgoat-devsecops.git
cd webgoat-devsecops
```

### 2. Build WebGoat

On Windows PowerShell:

```powershell
.\mvnw.cmd clean install -DskipTests
```

The Maven Wrapper downloads the required Maven version automatically.

A successful build creates the WebGoat JAR inside the `target` directory.

### 3. Build the Docker image

```bash
docker compose build
```

### 4. Start WebGoat

```bash
docker compose up -d
```

### 5. Check container status

```bash
docker compose ps
```

Wait until the container reports:

```text
(healthy)
```

## Access the Application

WebGoat:

`http://localhost:8080/WebGoat/`

WebWolf:

`http://localhost:9090/WebWolf/`

## Health Check

Check the application health from inside the container:

```bash
docker exec webgoat curl -s http://localhost:8080/WebGoat/actuator/health
```

The response should report:

```json
{"status":"UP"}
```

## Stop the Application

```bash
docker compose down
```

To also discard the container's database volume and start from a clean slate:

```bash
docker compose down -v
```

## Running the Tests

The test suite runs on the host with the Maven Wrapper — it does not need Docker.

Unit tests only:

```bash
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Full verification (unit tests, integration tests and the formatting check that
the build enforces):

```bash
./mvnw verify
```

A single test class:

```bash
./mvnw test -Dtest=SqlInjectionLesson5aTest
```

Surefire writes reports to `target/surefire-reports/`. Integration tests live in
`src/it/java` and include Playwright UI tests, which download a browser on first
run — expect the first `verify` to take noticeably longer.

If the build fails on formatting rather than on a test, apply the formatter and
re-run:

```bash
./mvnw spotless:apply
```

## Secrets and Configuration

No credentials are committed to this repository. Configuration reaches the
application through environment variables.

### Local development

`.env.example` is the committed template listing every variable the application
supports, with placeholder values only. Copy it and fill in real values locally:

```bash
cp .env.example .env
```

`.env` is gitignored (`.gitignore` ignores `.env` and `.env.*`, with an explicit
exception for `.env.example`). Never commit it. If you add a new variable, add
it to `.env.example` with a placeholder value in the same commit, so the rest of
the team knows it exists.

Every variable name in `.env.example` is read by
`src/main/resources/application-webgoat.properties` or
`application-webwolf.properties` — ports, bind addresses, context paths, the TLS
keystore and the optional GitHub OAuth client credentials used by the OAuth
lessons.

`docker-compose.yml` interpolates these as `${VARIABLE}` and passes them into the
container, so `docker compose up` picks up whatever is in `.env`. When a variable
is unset, compose falls back to the harmless development default declared inline
(`${VAR:-default}`), so nothing sensitive is ever hard-coded in the compose file.

### Pipeline

Secrets for CI are stored as **GitHub Actions encrypted secrets**
(Settings → Secrets and variables → Actions) and injected into workflow steps as
environment variables (`env: SECRET: ${{ secrets.SECRET }}`). They are never
written into workflow files, and never echoed to the job log.

### How a secret reaches each place

|      Destination      |                                                                    Path                                                                     |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| The running container | `.env` (gitignored) → compose `${VARIABLE}` interpolation → container environment → Spring reads it via `${VAR}` in the `.properties` files |
| The CI pipeline       | GitHub Actions encrypted secret → `${{ secrets.NAME }}` → step `env:` → tool/process                                                        |

Neither path stores a real value in git. The **Secrets - Gitleaks** gate in the
pipeline scans the whole working tree on every push and fails the build if a
credential is ever committed (see `docs/evidence/m4-failing-gate.txt`).

## Project Structure

```text
webgoat-devsecops/
├── src/                     # WebGoat source code
│   ├── main/                #   application code and resources
│   ├── test/                #   unit tests
│   └── it/                  #   integration and Playwright UI tests
├── config/                  # Checkstyle, dependency-check and desktop configs
├── docs/
│   ├── diagrams/            #   architecture diagram (draw.io source + PNG)
│   ├── evidence/            #   exploit screenshots and SAST scan output
│   └── report/              #   technical report drafts
├── .mvn/                    # Maven Wrapper files
├── .env.example             # Environment variable template (no real values)
├── Dockerfile               # Container definition
├── docker-compose.yml       # Container orchestration
├── pom.xml                  # Maven configuration
├── mvnw
├── mvnw.cmd
└── README.md
```

## Security Notice

OWASP WebGoat is intentionally vulnerable and is designed for security education
and testing. Do not run it on an untrusted network, and do not expose it to the
public internet.

The Docker Compose configuration publishes the application on `127.0.0.1` only,
so that WebGoat is not unintentionally exposed on external network interfaces.

## License and Attribution

This project is based on [OWASP WebGoat](https://github.com/WebGoat/WebGoat), an
intentionally vulnerable web application developed by the OWASP WebGoat project,
and is licensed under **GPL-2.0-or-later**. The upstream `LICENSE.txt`,
`COPYRIGHT.txt` and the per-file SPDX headers are retained unchanged.

As a derivative work, this repository — including our additions (the
`docker-compose.yml`, the CI/CD workflow, the secure-coding fixes and the
supporting documentation) — is also distributed under **GPL-2.0-or-later**.

The DevSecOps work in this repository (containerisation, threat modelling,
secure-coding fixes and the CI/CD pipeline) was carried out by the student
project team for the IE3142 DevOps Security module and is described in the
accompanying technical report.
