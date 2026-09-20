# Running project notes

The cumulative project decisions, command log and verified checkpoints are now
maintained in [the project README](../README.md) to avoid two competing histories.
The Java application is supplied; Mallik performs the DevOps/SRE work.

## September 20, 2026 checkpoint

Local JAR/API tests, local Docker startup/health/POST and initial GitHub CI have
passed. Identity Center group-based SSO for the sandbox is configured. A USD 100
budget was reported. Terraform returned the ECR URL and state bucket name.

Remote-backend/live-foundation verification, GitHub OIDC role creation, ECR
publication, AWS application deployment, EKS and expanded observability are not
marked complete without their verification outputs.

## Next action

Follow [the three-step OIDC/ECR runbook](checkpoint-oidc-ecr.md). Record the actual
plan summary, successful Actions run URL and ECR digest after they are verified.
Do not paste credentials, state files, saved plans or OIDC tokens into these notes.
