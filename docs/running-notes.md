# Running project notes

## Project agreement

Build a personal, enterprise-style DevOps/SRE lab at personal-lab scale alongside
Python interview preparation. The teacher provides and maintains the Java business
logic. Mallik performs and understands the build, runtime, container, Git, CI/CD,
AWS, monitoring, troubleshooting, Terraform and Kubernetes/Helm work.

Work one phase/checkpoint at a time: explain → perform → inspect output → verify →
record the result. No pre-generated Docker, CI/CD, or infrastructure solution is
included in the phase 1 handoff.

## Decisions in this starter

- Project/repository candidate name: `release-tracker`.
- Suggested local directory: `~/code/release-tracker` (not yet confirmed).
- Java build target: 21; Spring Boot: 3.5.16; Maven: 3.9.16 via wrapper 3.3.4.
- Initial application version: `0.1.0-SNAPSHOT`.
- Loopback-only service on port 8080; runtime configuration via environment variables.
- In-memory records for the first phase. Not durable or suitable for multiple replicas.
- Health and selected info endpoints only. Prometheus/Grafana is a later phase.
- Public exposure, authentication, TLS and durable persistence require later decisions.

## Current checkpoint

Source package prepared. User-machine build and runtime verification are pending.
Full Maven/Spring verification was unavailable in the preparation environment;
see `validation-status.md`.

| Checkpoint | Status | Evidence |
| --- | --- | --- |
| Extract source ZIP | Pending on user machine | |
| Confirm JDK/Maven | Pending on user machine | |
| Build with tests | Pending on user machine | |
| Inspect executable JAR | Pending on user machine | |
| Run JAR and check health | Pending on user machine | |
| POST and retrieve a release | Pending on user machine | |
| Stop application | Pending on user machine | |

## Session log

Append dates, commands, relevant outputs, explanations, and fixes here.
Avoid passwords, tokens, private keys, account identifiers, and work configuration.

## Future decisions — not blockers for local phase 1

AWS account/access setup, personal spending limit, Mac architecture/container
runtime, GitHub repository and registry, manual-first versus Terraform-first initial
AWS deployment, durable storage before multi-replica operation, and EKS lifecycle costs.

## Next action

Extract the ZIP and confirm `pwd`, `java -version`, `javac -version`, and
`./mvnw --version`. Then run the first build together.

## Checkpoint: local application and container validated
- Maven build completed successfully.
- Packaged JAR started successfully on the Mac.
- Local JAR API create/read operations succeeded.
- Built Docker image: release-tracker:0.1.0-local.
- Container published on 127.0.0.1:8080.
- Container health returned HTTP 200 with status UP.
- Container release creation returned HTTP 201.
- Next: GitHub source repository and initial CI.
