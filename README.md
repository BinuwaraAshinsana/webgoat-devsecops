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

`docs/architecture.png`

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

## Project Structure

```text
webgoat-devsecops/
├── src/                 # WebGoat source code
├── config/              # Application configuration
├── docs/                # Architecture documentation
├── .mvn/                # Maven Wrapper files
├── Dockerfile           # Container definition
├── docker-compose.yml   # Container orchestration
├── pom.xml              # Maven configuration
├── mvnw
├── mvnw.cmd
└── README.md
```

## Security Notice

OWASP WebGoat is intentionally vulnerable and is designed for security education and testing.

The Docker Compose configuration binds the application to `127.0.0.1` so that WebGoat is not intentionally exposed to external network interfaces.
