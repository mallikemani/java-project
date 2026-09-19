# Release Tracker — Phase 1

A small Java REST API for Mallik's hands-on DevOps/SRE project.
The application code is supplied. You own building, running, packaging,
delivering, and operating it as we progress through the lab.

**Current checkpoint: build and run the JAR locally.** No Docker, AWS,
GitHub Actions, Terraform, Helm, or Kubernetes configuration is included yet.

## What does it do?

Record an application's release, list releases, filter them by environment,
retrieve a release by ID, and delete a release. For example:

```json
{
  "applicationName": "payments-api",
  "version": "1.0.0-SNAPSHOT",
  "environment": "DEV",
  "status": "DEPLOYED"
}
```

This is only a record: posting `PROD` does not deploy anything to production.
The sample `payments-api` version is metadata, not the version of this Java service.
The Release Tracker service itself starts at `0.1.0-SNAPSHOT` in `pom.xml`.

## Phase 1 boundaries

- Records are held in memory and disappear on application restart. No database is required.
- No authentication or TLS is implemented. Bind to localhost only; do not expose this version publicly.
- No pagination, durable storage, multi-replica consistency, or production hardening is claimed.
- Health is exposed, but Prometheus/Grafana monitoring comes in its own phase.
- No compiled JAR, dependency cache, credentials, or Git history is included.

## Toolchain

The build targets JDK 21 and pins Spring Boot 3.5.16.
The included Apache Maven Wrapper 3.3.4 selects Maven 3.9.16.
The `mvnw` script supports macOS/Linux; a Windows launcher is not supplied in this Mac-first starter.
You need a JDK and Internet access for the initial tool/dependency downloads.
You do not need a separate Maven installation.

### Checkpoint A — inspect your machine

From this directory (the one containing `pom.xml`):

```bash
pwd
ls -la
java -version
javac -version
chmod +x mvnw
./mvnw --version
```

Use JDK 21 for the common baseline. If Java is missing, reports an older version,
or the wrapper fails, send the output back before changing things.
Do not overwrite any existing work-related Maven configuration or share credentials.

### Checkpoint B — build your first JAR

After the toolchain is confirmed:

```bash
./mvnw clean package
ls -lh target/release-tracker-0.1.0-SNAPSHOT.jar
```

`clean` removes this project's prior build output. `package` compiles the source,
runs tests, and packages the application. The Spring Boot plugin repackages the
JAR with its runtime dependencies. Maven may download dependencies on the first build.
Do not skip the tests. Continue only after `BUILD SUCCESS`.

The main artifact is `target/release-tracker-0.1.0-SNAPSHOT.jar`.
An additional `.jar.original` may appear; do not use that one for `java -jar`.

### Checkpoint C — run it in the foreground

```bash
java -jar target/release-tracker-0.1.0-SNAPSHOT.jar
```

Keep this terminal open. You should see the application start on port 8080.
The process continuing to run is expected; it has not hung.

In a second terminal:

```bash
curl -i http://127.0.0.1:8080/
curl -i http://127.0.0.1:8080/actuator/health
curl -i http://127.0.0.1:8080/api/releases
```

Expected: HTTP 200 for each call, health body `{"status":"UP"}`, and an empty
array `[]` for releases on a fresh start. The root endpoint returns JSON, not a web UI.

### Checkpoint D — record and retrieve a release

In the second terminal, change into this project directory first so the example
file's relative path resolves:

```bash
cd "$HOME/code/release-tracker"
curl -i -X POST http://127.0.0.1:8080/api/releases \
  -H 'Content-Type: application/json' \
  --data-binary @examples/create-release.json

curl -i http://127.0.0.1:8080/api/releases
```

Expect HTTP 201 from POST, with a generated `id` and `createdAt`, then HTTP 200
with the record in the list. A fresh POST creates a new ID every time.
The full endpoint reference is in `docs/phase-1-runbook.md`.

### Checkpoint E — stop it

In the terminal running Java, press **Ctrl+C**. Records will be lost.
That deliberate limitation becomes a persistence design topic in a later phase.

## What is where?

```text
release-tracker/
├── pom.xml                         Maven dependencies, version and build
├── mvnw                            Official Apache Maven launcher
├── .mvn/wrapper/                    Pinned Maven distribution
├── src/main/java/                  Application implementation
├── src/main/resources/             Runtime configuration
├── src/test/java/                  Unit and Spring HTTP-layer tests
├── examples/                       Valid/invalid JSON requests
└── docs/
    ├── phase-1-runbook.md           Local operations, API and troubleshooting
    ├── running-notes.md            Our continuing project notes
    ├── validation-status.md        What has and has not been verified
    └── references.md               Official documentation and wrapper provenance
```

## Before treating the build as verified

The archive, source syntax, wrapper syntax, and dependency-free domain logic were
checked during preparation. The full Maven build and Spring HTTP tests could not
be run in that preparation environment because downloads were unavailable.
Your `./mvnw clean package` and real HTTP checks are the first end-to-end verification.
See `docs/validation-status.md` for the exact distinction.

## Roadmap

Run manually → containerize → push to personal GitHub → CI → AWS → monitoring →
controlled failure → diagnose/recover → Terraform automation → Kubernetes/EKS.
We will resolve the manual-first versus Terraform-first AWS decision before phase 5.
