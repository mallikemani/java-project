# Next checkpoint — OIDC and ECR publication

Prepared September 20, 2026. These are the next actions, not a completed execution log.
All commands are run by Mallik; no remote GitHub/AWS changes were made by preparing
this bundle. Existing `main.tf`, `backend.tf`, Dockerfile and `ci.yml` are retained.

## Step 1 — Verify and commit the existing foundation plus updated notes

Download `release-tracker-next-checkpoint.zip` to Downloads, then extract it:

```bash
unzip "$HOME/Downloads/release-tracker-next-checkpoint.zip" -d "$HOME/Downloads"
cd "$HOME/code/cci-prep/release-tracker"
BUNDLE="$HOME/Downloads/release-tracker-next-checkpoint"
git branch --show-current
git status --short
```

Continue on the existing `infra/aws-foundation` branch. If the current branch is
not that branch, or unexpected local changes exist, stop and inspect rather than
switch branches blindly or discard changes. Do not recreate an existing branch.

The patch is based on the README and running notes read from GitHub `main`.
It will refuse conflicts rather than overwrite local edits. Review it first in an editor.

```bash
git apply --check "$BUNDLE/README.patch"
git apply "$BUNDLE/README.patch"
cp -i "$BUNDLE/docs/checkpoint-oidc-ecr.md" docs/checkpoint-oidc-ecr.md
export AWS_PROFILE=lab-sandbox
export AWS_REGION=us-west-2
export AWS_DEFAULT_REGION=us-west-2
aws sts get-caller-identity --profile lab-sandbox --no-cli-pager
```

The account must be `513594860053`. If SSO has expired, renew then rerun identity:

```bash
aws sso login --profile lab-sandbox
```

Check the initialized backend type without displaying its credential/config fields:

```bash
grep '"type": "s3"' infra/foundation/.terraform/terraform.tfstate
```

Expect the `"type": "s3"` line. If absent, or the file is missing, stop and finish
backend migration from the previous checkpoint. Do not publish the metadata file.

Verify the remote state object's metadata, without downloading/sharing the state:

```bash
aws s3api head-object \
  --bucket java-project-tfstate-513594860053-us-west-2 \
  --key foundation/terraform.tfstate \
  --profile lab-sandbox --region us-west-2 \
  --query '{Bytes:ContentLength,Version:VersionId,Modified:LastModified}' \
  --output json --no-cli-pager
terraform -chdir=infra/foundation plan
```

Expect metadata for an existing non-empty state object and `No changes` in the plan.
`terraform output` alone did not prove these checks. If the object does not exist,
access is denied or the plan differs, stop before adding OIDC resources.

Stage only the intended source, lockfile and documentation, not state or plans:

```bash
git add README.md docs/running-notes.md docs/checkpoint-oidc-ecr.md \
  infra/foundation/main.tf infra/foundation/backend.tf \
  infra/foundation/.terraform.lock.hcl
git diff --cached --stat
git diff --cached
git commit -m "Document lab progress and version AWS foundation"
```

If the safe documentation patch reports a conflict because you added local notes,
merge the supplied README/notes manually. Do not use reset/checkout to erase your notes.

## Step 2 — Create the dedicated GitHub image-publishing role

Populate `infra/foundation/github-oidc.tf` using the supplied file. Review it before
copying; it adds three resource blocks and two outputs. It reuses the existing
provider, account guard, local variables and ECR resource. Do not replace `main.tf`.

```bash
cp -i "$BUNDLE/infra/foundation/github-oidc.tf" infra/foundation/github-oidc.tf
aws iam list-open-id-connect-providers \
  --profile lab-sandbox \
  --query 'OpenIDConnectProviderList[].Arn' \
  --output text --no-cli-pager
```

On this new sandbox there should not yet be a GitHub provider. If the output
already includes `oidc-provider/token.actions.githubusercontent.com`, stop for a
state/import review rather than deleting or duplicating the provider. Other OIDC
providers are distinct and must not be removed.

```bash
terraform -chdir=infra/foundation fmt
terraform -chdir=infra/foundation validate
terraform -chdir=infra/foundation plan -out=oidc.tfplan
```

Expected for a fresh GitHub setup: `3 to add, 0 to change, 0 to destroy` — the OIDC
provider, IAM role and its inline policy. If existing S3/ECR resources are unexpectedly
changing or being replaced, do not apply until the difference is explained.

After reviewing the plan:

```bash
terraform -chdir=infra/foundation apply oidc.tfplan
terraform -chdir=infra/foundation output github_ecr_publish_role_arn
terraform -chdir=infra/foundation output github_oidc_main_subject
terraform -chdir=infra/foundation plan
```

Expected role:
`arn:aws:iam::513594860053:role/release-tracker-github-ecr-publish`

Security model:

- Your human uses SSO; GitHub uses an OIDC token exchanged for temporary STS credentials.
- Audience is exactly `sts.amazonaws.com`.
- Trust is limited to this repository's `main` subject; no branch/repo wildcards.
- New GitHub repositories use an immutable subject including owner/repository IDs.
  This policy uses the actual connected repository IDs. The supplied workflow
  checks and prints only the non-secret `sub`/`aud` claims before authentication.
- The ECR token operation requires wildcard resource scope. Image operations are
  restricted to this single repository ARN; DescribeImages supports digest verification.
- The role cannot provision infrastructure, change IAM, delete images or access
  Terraform state. It is not BootstrapAdmin.
- Trust covers matching main-branch workflows, not just one filename. Protect main;
  a future environment/custom-subject policy must be designed deliberately.
- No GitHub Environment is set in this workflow; adding one changes the subject.

This manual SSO apply bootstraps trust before GitHub can authenticate. A later,
separate infrastructure role/workflow will manage Terraform; do not broaden the
image-publishing role to make it do infrastructure administration.

## Step 3 — Add the publication workflow, merge and verify the digest

Populate `.github/workflows/publish.yml` from the supplied file; preserve `ci.yml`.
Read the workflow's ordered steps. No GitHub AWS access-key secrets are required.

```bash
cp -i "$BUNDLE/.github/workflows/publish.yml" .github/workflows/publish.yml
git add infra/foundation/github-oidc.tf .github/workflows/publish.yml
git diff --cached --check
git diff --cached --stat
git diff --cached
git commit -m "Add restricted GitHub OIDC and tested ECR publishing"
git push -u origin infra/aws-foundation
```

Browser: open PR `infra/aws-foundation` → `main`. Review source changes and wait for
existing `Release Tracker CI` to pass, then merge. `Publish to ECR` runs on the
resulting push to main (or a manual dispatch on main), not during a PR. This means
the new publishing path receives its first end-to-end test after merge. Do not
broaden the trust policy to publish untrusted PR code just to test it sooner.

The publication workflow repeats tests independently before publishing; it does
not rely on the concurrently triggered CI workflow to gate its steps. The current
simple design deliberately duplicates those checks. Later we can consolidate the
jobs while preserving tested-artifact provenance and separate permissions.

Flow:

```text
Checkout → Java tests/package → unique tag → build linux/amd64 image
  → start exact image → health + create/read smoke tests → stop test container
  → verify OIDC subject → obtain temporary AWS role credentials
  → ECR login → push exact tested image → record remote image digest
```

Actions are pinned to full commit hashes. The tag includes source SHA, run ID and
run attempt so a re-run does not overwrite an immutable tag. This is one platform,
not a multi-architecture manifest. Future EKS nodes must match or the build must
be extended deliberately. Build-once means the image tested in this publication
job is the image pushed; it does not guarantee bit-for-bit reproducibility across
separate runs using a mutable base-image tag.

After a successful publication, use your Mac to verify:

```bash
aws ecr describe-images \
  --repository-name release-tracker \
  --profile lab-sandbox --region us-west-2 \
  --query 'sort_by(imageDetails,&imagePushedAt)[].{Tags:imageTags,Digest:imageDigest,Pushed:imagePushedAt}' \
  --output json --no-cli-pager
```

Compare the tag/digest with the workflow's `Published image` summary. Do not assume
an older image proves the new run succeeded. Send the successful run URL and that
matching record (not JWTs or credentials). Then record those actual results in
the README/notes. Do not label the image scan clean merely because a push succeeded;
scan results, supply-chain checks and a release gate are subsequent work.

After the PR merge, provided local work is committed:

```bash
git status --short
git switch main
git pull --ff-only origin main
```

### Stops and expected failures

- Wrong profile/account or an expired SSO session: fix local authentication first.
- Terraform requests state migration: finish the existing migration; do not create
  a second state for the same foundation.
- OIDC claim check fails: share only printed `sub`/`aud`; review actual customization
  and match a precise subject. Never print a full token or change trust to `*`.
- `AssumeRoleWithWebIdentity` fails: inspect provider/audience/subject/role, including
  IAM propagation. Do not solve it by creating static AWS keys.
- Image push access denied: compare the exact repository ARN and action policy.
- A failed smoke test prevents authentication/push and prints the test container logs.
- A stopped local Docker container or a token cache is not proof of AWS deployment.

### Teardown boundary

This checkpoint adds identity resources and an image upload, not EC2 or EKS.
Images and state still have storage/request costs. Do not destroy the foundation
just to stop future compute; compute gets a separate Terraform state and cleanup
checkpoint. No account spend cap or automatic teardown was created by these files.
