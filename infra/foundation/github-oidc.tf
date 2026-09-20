# Add this file beside the existing main.tf and backend.tf.
# Reuses local.account_id, local.region and aws_ecr_repository.release_tracker.
# No access to management-account resources or Terraform state is granted to CI.

locals {
  # Verified repository/owner IDs from the connected GitHub repository.
  # New GitHub repositories use the immutable subject format documented in 2026.
  github_main_subject = "repo:mallikemani@73798184/java-project@1377604220:ref:refs/heads/main"
}

resource "aws_iam_openid_connect_provider" "github_actions" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]

  # For GitHub, AWS validates certificates through its trusted CA library.
  # Do not copy a stale hard-coded certificate thumbprint from an old tutorial.
}

resource "aws_iam_role" "github_ecr_publish" {
  name                 = "release-tracker-github-ecr-publish"
  description          = "Allow java-project main workflows to publish release-tracker images only"
  max_session_duration = 3600

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = aws_iam_openid_connect_provider.github_actions.arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          "token.actions.githubusercontent.com:sub" = local.github_main_subject
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "github_ecr_publish" {
  name = "publish-release-tracker-images"
  role = aws_iam_role.github_ecr_publish.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "AuthenticateToECR"
        Effect   = "Allow"
        Action   = ["ecr:GetAuthorizationToken"]
        Resource = "*"
      },
      {
        Sid    = "PublishAndInspectOnlyThisRepository"
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:PutImage",
          "ecr:BatchGetImage",
          "ecr:DescribeImages"
        ]
        Resource = aws_ecr_repository.release_tracker.arn
      }
    ]
  })
}

output "github_ecr_publish_role_arn" {
  value = aws_iam_role.github_ecr_publish.arn
}

output "github_oidc_main_subject" {
  value = local.github_main_subject
}
