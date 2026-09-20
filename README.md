# Release Tracker — DevOps/SRE Lab

A Java REST API used for a hands-on, enterprise-style DevOps/SRE project at personal-lab scale.
The Java business logic is supplied; Mallik builds, packages, delivers, configures,
observes and operates the service. Work progresses through reviewed checkpoints.

**Current checkpoint:** local JAR and container validated; initial GitHub Actions CI
passed; AWS sandbox SSO configured; Terraform has reported the ECR repository URL
and state-bucket name. Live foundation/backend verification is the next check.
**Next checkpoint:** restricted GitHub OIDC role and publishing a tested image to ECR.
Neither that publication nor an AWS application deployment has been verified yet.

## Project map

| Item | Current setting / evidence |
| --- | --- |
| Repository | `mallikemani/java-project` |
| Local project root | `~/code/cci-prep/release-tracker` |
| Application | `release-tracker`, version `0.1.0-SNAPSHOT` |
| Build | Java 21; Spring Boot 3.5.16; Maven Wrapper 3.3.4 selecting Maven 3.9.16 |
| Local container | `release-tracker:0.1.0-local`, published on `127.0.0.1:8080` |
| Terraform CLI observed | `1.15.8`, `darwin_arm64` |
| AWS CLI observed | `2.36.49`, macOS ARM64 |
| Human sign-in | Identity Center user `venkata_emani`, with device-based MFA reported |
| Group-based account access | `lab-bootstrap-admins` → `BootstrapAdmin`; direct user assignment removed |
| SSO session | `personal-lab` |
| Management profile | `lab-bootstrap` — organization/bootstrap work only |
| Sandbox profile | `lab-sandbox` |
| Sandbox AWS account | `513594860053` |
| Workload Region | `us-west-2` |
| Monthly AWS budget | USD 100, reported configured by Mallik; an alert, not a spending cap |
| Registry output | `513594860053.dkr.ecr.us-west-2.amazonaws.com/release-tracker` |
| State bucket output | `java-project-tfstate-513594860053-us-west-2` |
| Intended remote state key | `foundation/terraform.tfstate` |

Account IDs and role ARNs identify resources; they are not passwords. Still omit
unneeded personal/account data from logs. Never commit tokens, credentials, MFA
material, Terraform state, saved plans, or private keys.

## Application behavior and boundaries

The API records an application's release, lists releases, filters by environment,
retrieves a release by ID, and deletes a release. Example request:

```json
{
  "applicationName": "payments-api",
  "version": "1.0.0-SNAPSHOT",
  "environment": "DEV",
  "status": "DEPLOYED"
}
```

This records metadata; it does not deploy `payments-api`. Its version is distinct
from the Release Tracker service's own `0.1.0-SNAPSHOT` version.

Records are in memory and disappear when the Java process restarts. Authentication,
TLS, pagination, durable storage and multi-replica consistency are not implemented.
Do not expose this version publicly or treat independent replicas as a consistent
shared database. The root endpoint returns JSON, not a web user interface.

The default JAR binds to `127.0.0.1:8080`. The Dockerfile overrides
`SERVER_ADDRESS=0.0.0.0` inside the container, while the local Docker publish rule
binds the host port to `127.0.0.1`. Prometheus and OpenTelemetry instrumentation
are later checkpoints; the existing health endpoint is not a full monitoring system.

See [the phase 1 runbook](docs/phase-1-runbook.md) for the original API reference.
The starter's preparation-time validation document describes the original handoff;
the verified local/CI outcomes below supersede its pending-build status.

## Scope — retained and expanded

| Area | Progress / planned learning |
| --- | --- |
| Java build and local runtime | Validated locally; initial CI reported 20 passing tests |
| Containerization | Non-root runtime image built and exercised locally |
| Git and CI | Initial PR merged; Java tests and image build automated |
| AWS administration | Organization, sandbox, Identity Center, group access and budget established by user |
| Infrastructure as code | S3/ECR foundation outputs reported; backend/live checks pending |
| Image delivery | OIDC and ECR publishing are the next checkpoint, not completed |
| AWS deployment | First AWS deployment, then the same application's EKS progression; provision only after cost review |
| Kubernetes / Helm | Workload manifests, probes, RBAC, resource limits, rollout/recovery, scaling and Helm packaging |
| Observability | OpenTelemetry instrumentation and Collector; Prometheus metrics/PromQL; Grafana dashboards; logs/traces, retention and access controls |
| SRE practice | Request latency/errors/traffic/saturation, SLIs/SLOs, error budgets, alerting, load, controlled failures, diagnosis, recovery and incident notes |
| Operations and teardown | GitHub-based infrastructure workflow, separate identities, reviewed changes, reproducible environments and verified cleanup |

Terraform has been introduced before the first AWS workload deployment. The original
manual-first-versus-IaC decision is no longer unresolved for the foundation.
The application, AWS, EKS, Helm and operational scope remains intact; observability
expands that scope rather than replacing it. No claim of full production readiness
or a guaranteed final cost is made.

## Command log and replay notes

This is a cleaned, chronological command runbook reconstructed from the conversation,
posted terminal output, and the reviewed CI run. It is **not a verbatim shell history**.
Commands prescribed in the walkthrough are retained even when their individual
execution was not pasted; each section identifies the evidence actually seen.
Do not replay completed initialization/provisioning commands blindly.

File population is deliberately recorded as a short action. The full Java, Docker,
Terraform and workflow contents belong in their version-controlled files, not here.
Commands below run from the project root unless a different directory is stated.

### 01 — Extract the starter and inspect the machine

Extraction was confirmed by the user's directory listing. This is the replay path;
the exact original unzip command was not pasted.

```bash
mkdir -p "$HOME/code/cci-prep"
unzip "$HOME/Downloads/release-tracker-phase1.zip" -d "$HOME/code/cci-prep"
cd "$HOME/code/cci-prep/release-tracker"
pwd
ls -la
uname -m
sw_vers -productVersion
brew --version
java --version
javac --version
which java
mvn --version
```

Observed initially: ARM64 Mac; Homebrew available; Java launcher existed at
`/usr/bin/java` but could not locate a runtime; standalone `mvn` was unavailable.
The shell aliases/typos `l`, `mvnw --version` without `./`, and `whcih java` also
appeared. Use `ls -la`, `./mvnw --version`, and `which java` respectively.
There is no reason to replace or delete macOS `/usr/bin/java`.

### 02 — Install/select Java and use the Maven wrapper

```bash
brew install --cask temurin@21
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
java --version
javac --version
chmod +x mvnw
./mvnw --version
```

Confirmed from the installation transcript: Temurin installed successfully,
Java/Javac reported 21.0.12.1, and Maven Wrapper selected Maven 3.9.16.
`JAVA_HOME` and `PATH` exports affect this shell; persistent shell setup has not
been demonstrated. No standalone Maven installation was required.

Homebrew cleanup removed `python@3.13` in that transcript. The suggested follow-up
was `python3 --version`; a successful follow-up result has not been supplied.
Do not treat AWS CLI's embedded Python version as proof of a standalone Python setup.

### 03 — Build and run the executable JAR

```bash
cd "$HOME/code/cci-prep/release-tracker"
./mvnw clean package
ls -lh target/
ls -lh target/release-tracker-0.1.0-SNAPSHOT.jar
java -jar target/release-tracker-0.1.0-SNAPSHOT.jar
```

The user also navigated with `cd target`, inspected its listing, and used `cd ..`
to return before running the JAR. Confirmed: `BUILD SUCCESS`; executable JAR
produced; Spring Boot started on port 8080. The `.jar.original` is the artifact
before executable repackaging, not the one to run.

In a second terminal:

```bash
curl -i http://127.0.0.1:8080/actuator/health
curl -i -X POST http://127.0.0.1:8080/api/releases \
  -H 'Content-Type: application/json' \
  -d '{
    "applicationName": "payments-api",
    "version": "1.0.0-SNAPSHOT",
    "environment": "DEV",
    "status": "DEPLOYED"
  }'
curl -i http://127.0.0.1:8080/api/releases
```

Confirmed for the local JAR: POST returned HTTP 201; the list response contained
the created record; the application log contained the matching `release_created` ID.
Press **Ctrl+C** in the Java terminal to stop the process before reusing host port 8080.

### 04 — Set up Docker, package the JAR and run the container

The successful image/container outputs were pasted. The installation branch below
was prescribed only if Docker Desktop was absent; its actual installation command
was not separately demonstrated.

```bash
docker version
# Only if Docker Desktop was missing:
brew install --cask docker-desktop
# Open the existing/just-installed app:
open -a Docker
docker version
cd "$HOME/code/cci-prep/release-tracker"
```

- Populate `Dockerfile` with the Java 21 runtime, non-root UID/GID 10001, JAR copy,
  container bind address, port documentation and exec-form Java entrypoint.
- Populate `.dockerignore` to send only the necessary build files and executable
  JAR to Docker; do not exclude the JAR required by `COPY`.

```bash
docker build --pull -t release-tracker:0.1.0-local .
docker image ls release-tracker
docker run --detach \
  --name release-tracker \
  --publish 127.0.0.1:8080:8080 \
  release-tracker:0.1.0-local
docker logs --follow --tail 30 release-tracker
```

After observing startup, **Ctrl+C** exits log-following; the detached container
continues running.

```bash
docker ps --filter name=release-tracker
docker ps
curl -i http://127.0.0.1:8080/actuator/health
curl -i -X POST http://127.0.0.1:8080/api/releases \
  -H 'Content-Type: application/json' \
  -d '{
    "applicationName": "payments-api",
    "version": "1.0.0-SNAPSHOT",
    "environment": "DEV",
    "status": "DEPLOYED"
  }'
curl -i http://127.0.0.1:8080/api/releases
```

Confirmed in the pasted container output: container `Up`, localhost-only port
mapping, health HTTP 200 / `UP`, and release creation HTTP 201.
The final container GET response was requested but not pasted; do not record it
as independently verified. A Linux-hosted CI container smoke test is a next step.

An additional file-ownership teaching check was suggested:

```bash
id -Gn mallikrajaemani
```

`ls -l` identifies the file's owning user and owning group; it is not the user's
complete group-membership listing. That command's result was not supplied.

### 05 — Initialize Git, push the repository and add initial CI

Prescribed sequence; the resulting repository, workflow and merged PR were read
through the connected GitHub tool. The actual remote authentication transport was
not pasted; this replay example uses HTTPS. Do not re-add an existing `origin`.

```bash
cd "$HOME/code/cci-prep/release-tracker"
git init -b main
```

- Populate/update `.gitignore` with build, local-secret, Terraform state/plan and
  provider-cache exclusions; retain `.terraform.lock.hcl` for version control.
- Update `docs/running-notes.md` with the local build/container checkpoint.

```bash
git add .
git diff --cached --stat
git diff --cached
git commit -m "Bootstrap release tracker with validated local container"
git remote add origin https://github.com/mallikemani/java-project.git
git remote -v
git push -u origin main
git switch -c ci/initial-pipeline
mkdir -p .github/workflows
```

- Populate `.github/workflows/ci.yml` with checkout, Temurin Java 21, Maven wrapper
  test/package and Docker image build; pin official actions to reviewed commits.

```bash
git add .github/workflows/ci.yml
git commit -m "Add Java tests and Docker image build to CI"
git push -u origin ci/initial-pipeline
```

Browser action: open a PR to `main`, review the passing initial job, then merge PR #1.
The reviewed run was `35469135081`, job `105966772665`; it reported 20 tests,
zero failures/errors/skips, Maven 3.9.16, successful JAR packaging and image build.
It did not publish the image or start a Docker container for a smoke test.

```bash
git status --short
git switch main
git pull --ff-only origin main
```

Repository: <https://github.com/mallikemani/java-project>
Initial CI evidence: <https://github.com/mallikemani/java-project/actions/runs/35469135081>

### 06 — Install AWS CLI and check Terraform

```bash
aws --version
terraform version
curl --fail --location \
  "https://awscli.amazonaws.com/AWSCLIV2.pkg" \
  --output "$HOME/Downloads/AWSCLIV2.pkg"
sudo installer -pkg "$HOME/Downloads/AWSCLIV2.pkg" -target /
which aws
aws --version
```

The first AWS check failed because `aws` was missing. Subsequent output confirmed
AWS CLI v2.36.49. Terraform v1.15.8 was already available; no Terraform upgrade was
performed in the supplied transcript. The CLI installer sequence above was supplied
in the walkthrough; individual installer output was not pasted.

### 07 — Confirm identity and establish non-root access

Initially, this read-only command was run in browser CloudShell, whose session
used the console identity, not in the unauthenticated Mac terminal:

```bash
aws sts get-caller-identity --query Arn --output text --no-cli-pager
```

The user confirmed the result was root. Console work then established an
organization instance of IAM Identity Center, user `venkata_emani`, device-based
MFA, group membership, and permission set `BootstrapAdmin` with administrative
bootstrap access. The direct user-to-account assignment was removed in favor of
the group assignment. Do not create root keys or duplicate IAM users for this flow.

Conceptual mapping:

```text
Organization root (container, not a login)
  ├── Management AWS account: mallik_raja
  └── Member AWS account: lab-sandbox

Identity Center user venkata_emani
  → member of lab-bootstrap-admins
  → account assignment + BootstrapAdmin permission set
  → temporary assumed-role credentials in the selected account
```

Mac setup:

```bash
aws configure sso --profile lab-bootstrap
aws sts get-caller-identity \
  --profile lab-bootstrap \
  --query Arn \
  --output text \
  --no-cli-pager
```

Confirmed: an `AWSReservedSSO_BootstrapAdmin_.../venkata_emani` assumed-role ARN,
not root. The working session name is `personal-lab`. Use the existing SSO start
URL and actual Identity Center Region; do not publish private login/MFA material.
For future expiry, the prescribed renewal command is:

```bash
aws sso login --profile lab-bootstrap
```

### 08 — Create sandbox account, assign the group and add its CLI profile

Console actions: create member account `lab-sandbox` with a unique account-owner
email; assign the existing group and BootstrapAdmin permission set; create a
monthly USD 100 organization-level cost budget with email notifications.
These actions were reported by the user, not inspected through an AWS connection.
No new Identity Center user or instance is required per AWS account.

```bash
aws configure get sso_session --profile lab-bootstrap
aws configure sso --profile lab-sandbox
aws sts get-caller-identity \
  --profile lab-sandbox \
  --query '{Account:Account,Arn:Arn}' \
  --output json \
  --no-cli-pager
```

Confirmed: SSO session `personal-lab`; sandbox account `513594860053`;
role `BootstrapAdmin`; default client Region `us-west-2`; assumed-role identity
ending in `/venkata_emani`. Workload Region was therefore standardized on
`us-west-2`, superseding the earlier `us-east-1` suggestion.

### 09 — Bootstrap Terraform foundation locally

These are the foundation commands provided in the immediately preceding checkpoint.
The latest user output confirms the two Terraform outputs below; the apply summary,
post-migration no-change plan and S3 object verification have not yet been supplied.
Do not infer active remote-state use solely from `terraform output`.

```bash
cd "$HOME/code/cci-prep/release-tracker"
git status --short
git switch main
git pull --ff-only origin main
git switch -c infra/aws-foundation
mkdir -p infra/foundation
export AWS_PROFILE=lab-sandbox
export AWS_REGION=us-west-2
export AWS_DEFAULT_REGION=us-west-2
```

- Populate `infra/foundation/main.tf` with the Terraform/provider requirements,
  sandbox-account guard, default tags, private/versioned/encrypted state bucket,
  TLS-only bucket policy, immutable private ECR repository, scan-on-push request
  and outputs. The initial bootstrap configuration uses local state.

```bash
terraform -chdir=infra/foundation fmt
terraform -chdir=infra/foundation init
terraform -chdir=infra/foundation validate
terraform -chdir=infra/foundation plan -out=bootstrap.tfplan
# Review the saved plan before applying it.
terraform -chdir=infra/foundation apply bootstrap.tfplan
```

Expected for the original fresh configuration: 6 resources to add, no changes or
deletions. This is a count of Terraform resource blocks, not six servers/buckets.
`apply bootstrap.tfplan` applies the approved saved plan without a second yes/no
prompt; review first. Local bootstrap state stays out of Git.

### 10 — Migrate the foundation state to S3

Only after the state bucket was created:

- Populate `infra/foundation/backend.tf` with the S3 bucket/key/Region,
  `encrypt = true`, `use_lockfile = true` and allowed sandbox account ID.

```bash
terraform -chdir=infra/foundation fmt
terraform -chdir=infra/foundation init -migrate-state
# Review the migration prompt; answer yes to copy the existing local state to S3.
terraform -chdir=infra/foundation plan
terraform -chdir=infra/foundation output
```

Reported outputs:

```text
ecr_repository_url = "513594860053.dkr.ecr.us-west-2.amazonaws.com/release-tracker"
state_bucket_name = "java-project-tfstate-513594860053-us-west-2"
```

The follow-up verification should show an active S3 backend, the remote state
object and a no-change plan. Keep local state/backup files private while migration
is verified; do not delete them or use `-reconfigure` as a substitute for migration.
The state bucket is protected from an ordinary destroy; disposable compute will
be managed separately. Final account cleanup will require its own deliberate plan.

## Next three steps — not yet executed

Use [the OIDC/ECR checkpoint runbook](docs/checkpoint-oidc-ecr.md).

1. Verify the remote-state foundation and commit this README plus the existing
   foundation code and dependency lockfile.
2. Populate `infra/foundation/github-oidc.tf` with a GitHub provider, a role scoped
   to this repository's `main` subject, and permissions limited to publishing and
   inspecting images in the existing ECR repository. Review and apply the plan.
3. Populate `.github/workflows/publish.yml`; PR and merge the changes after initial
   CI passes. The publication workflow will test, build, smoke-test, authenticate
   through OIDC and push a uniquely tagged image. Verify the ECR digest afterwards.

These supplied files are prospective configuration, not proof that access was
created or publication succeeded. The README will be updated after actual results.
No AWS access keys need to be stored in GitHub, and this role must not receive
AdministratorAccess or Terraform-state access.

## Practical operating rules

Keep credentials/state/plans out of Git. Use `--profile lab-sandbox` or explicitly
select it for Terraform; never assume a terminal's default account is correct.
Use reviewable commits, plans and PRs; do not force-push or apply surprise deletions.
Build and publish images in CI for a selected CPU architecture, then deploy the
verified digest. The current local image on the Mac may be ARM64; the supplied
next publication workflow deliberately builds `linux/amd64` for matching AWS nodes.

The budget is an alert, not a hard limit. Do not provision EKS/EC2/NAT/load balancers
until the resource plan, expected runtime and cleanup plan are agreed. OpenTelemetry,
Prometheus/Grafana retention/cardinality and AWS billing controls remain part of the
project, not optional afterthoughts. No automated teardown task has been scheduled.

## Reference material

Session-specific progress comes from the conversation, its supplied terminal
transcripts and the connected repository reads. Technical implementation references:

- Maven Wrapper: <https://maven.apache.org/wrapper/>
- AWS CLI SSO: <https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-sso.html>
- Terraform S3 backend: <https://developer.hashicorp.com/terraform/language/backend/s3>
- GitHub OIDC with AWS: <https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws>
- GitHub subject formats: <https://docs.github.com/en/actions/reference/security/oidc>
- ECR push permissions: <https://docs.aws.amazon.com/AmazonECR/latest/userguide/image-push-iam.html>
- ECR push process: <https://docs.aws.amazon.com/AmazonECR/latest/userguide/docker-push-ecr-image.html>

Last documentation checkpoint: September 20, 2026. Update evidence/status after
running the next commands; do not mark upcoming milestones complete preemptively.
