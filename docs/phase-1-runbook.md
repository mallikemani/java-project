# Phase 1 runbook — local JAR

Use the README checkpoints one at a time. This document is a reference, not extra
work to finish in the first session.

## Application contract

The service needs a compatible Java runtime, memory, and an available TCP port.
It needs no database or AWS credentials in phase 1. It writes logs to the console.
It binds to `127.0.0.1:8080` by default and uses process-local memory for records.

| Setting | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP listening port |
| `SERVER_ADDRESS` | `127.0.0.1` | Listening interface; keep localhost in phase 1 |
| `APP_ENV` | `local` | Runtime environment label returned by `/` and `/actuator/info` |
| `APP_LOG_LEVEL` | `INFO` | Logging level for the application package |

These are environment variables, not secrets. Do not place actual credentials in
`application.yml`, shell examples, project notes, or Git.
The `environment` field in a release record is independent of the service's `APP_ENV`.

Later, after the baseline works, we can run the **same JAR** on a different port:

```bash
SERVER_PORT=8081 APP_ENV=local-lab java -jar target/release-tracker-0.1.0-SNAPSHOT.jar
```

No source edit or rebuild is needed for this runtime configuration change.

## HTTP API

| Method | Path | Expected success |
| --- | --- | --- |
| GET | `/` | 200, application identity/version/environment |
| GET | `/actuator/health` | 200, `{"status":"UP"}` when healthy |
| GET | `/actuator/info` | 200, selected application metadata |
| POST | `/api/releases` | 201 with JSON record and Location header |
| GET | `/api/releases` | 200 with newest-first array; ties ordered by ID |
| GET | `/api/releases?environment=DEV` | 200 with matching records |
| GET | `/api/releases/{id}` | 200 with one record |
| DELETE | `/api/releases/{id}` | 204 with no response body |

There is no status-update endpoint in this starter. IDs are server-generated UUIDs.
For POST, all four input fields are required. `environment` must be `DEV`, `QA`,
or `PROD`; `status` must be `PENDING`, `DEPLOYED`, or `FAILED`. Enum values are
case-sensitive. Names and versions are limited to 64 characters and restricted
to the characters documented in `CreateReleaseRequest.java`.

Invalid input or malformed JSON returns HTTP 400. A valid but nonexistent UUID
returns HTTP 404. Invalid UUID syntax returns HTTP 400. The application handlers
return Problem Detail JSON without stack traces or the submitted payload.

### Validation check

Run from the project root:

```bash
curl -i -X POST http://127.0.0.1:8080/api/releases \
  -H 'Content-Type: application/json' \
  --data-binary @examples/invalid-release.json
```

Expected: HTTP 400, not 201. This demonstrates input rejection, not an application outage.

### Read or delete by ID

Replace the example value below with the `id` from an actual POST response:

```bash
RELEASE_ID='paste-the-returned-uuid-here'
curl -i "http://127.0.0.1:8080/api/releases/$RELEASE_ID"
curl -i -X DELETE "http://127.0.0.1:8080/api/releases/$RELEASE_ID"
```

### Filter

```bash
curl -i 'http://127.0.0.1:8080/api/releases?environment=DEV'
```

## Maven commands we will learn

| Command | Purpose in this project |
| --- | --- |
| `./mvnw --version` | Show the selected Maven and Java versions |
| `./mvnw clean` | Remove prior build output under `target/` |
| `./mvnw test` | Compile and run the supplied tests |
| `./mvnw clean package` | Clean, test, and build the executable JAR |
| `./mvnw clean verify` | Run the lifecycle through verify, including packaging/tests |

There is no separate Failsafe integration-test phase configured yet. Both the
store tests and Spring MockMvc HTTP-layer tests run through Surefire in `test`.
MockMvc exercises the HTTP layer in the application context; it does not prove
that the packaged JAR starts and accepts real network requests. The manual curl
checks verify that additional boundary.

Test reports appear under `target/surefire-reports/` after the tests run.
The source contains 8 store unit tests and 12 Spring HTTP-layer tests.
No external Maven repository publication is configured. `install` and `deploy`
are not needed to run this application locally.

## First troubleshooting checks

**Java missing or wrong version:** capture `java -version`, `javac -version`,
and, on macOS, `/usr/libexec/java_home -V`. We will select/install JDK 21 before
changing your global setup. A JRE alone is not enough for compilation.

**Permission denied for mvnw:** from the project root, run `chmod +x mvnw`.

**Wrapper/dependency download failure:** capture the failing URL, error, and
last 30–50 log lines. Separate DNS/proxy/TLS/network failures from Java compiler
or test failures. Do not disable TLS verification, delete your entire `.m2`, or
paste your Maven settings file. Existing Maven mirror settings may affect resolution.

**No pom.xml:** run `pwd` and `ls -la`; change to the directory containing `pom.xml`.

**Port 8080 in use:** on macOS inspect `lsof -nP -iTCP:8080 -sTCP:LISTEN`.
Do not kill an unknown process. Stop your own previous app normally or use 8081.

**Unable to access JAR:** check that the build completed and inspect `ls -lh target/`.
Use the exact `.jar` name, not `.jar.original`.

**Connection refused:** confirm that the Java process is still running, the start
log shows the expected port, and your curl uses that port. Check startup errors.

**Empty records after restart:** expected for this in-memory version, not a bug.

**HTTP 404 at an unfamiliar route:** use one of the API paths above; there is no browser UI.

## Before the next phase

We should have evidence of the selected toolchain, a successful build with tests,
a locally built JAR, successful health/POST/GET calls, and a graceful stop.
Be able to explain which port the service uses, where logs go, and why its records
vanish on restart. Record evidence in `running-notes.md`; do not mark steps done
until they have actually succeeded on your Mac.
