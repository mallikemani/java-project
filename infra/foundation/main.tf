terraform {
  required_version = "= 1.15.8"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

locals {
  account_id = "513594860053"
  region     = "us-west-2"
}

provider "aws" {
  region = local.region

  # Refuse to operate in an account other than our sandbox.
  allowed_account_ids = [local.account_id]

  default_tags {
    tags = {
      Project     = "release-tracker"
      Environment = "sandbox"
      ManagedBy   = "Terraform"
    }
  }
}

# Terraform state storage.
resource "aws_s3_bucket" "terraform_state" {
  bucket        = "java-project-tfstate-${local.account_id}-${local.region}"
  force_destroy = false

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_versioning" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_policy" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  # Deny non-TLS requests from non-service principals.
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "DenyInsecureTransport"
      Effect    = "Deny"
      Principal = "*"
      Action    = "s3:*"
      Resource = [
        aws_s3_bucket.terraform_state.arn,
        "${aws_s3_bucket.terraform_state.arn}/*"
      ]
      Condition = {
        Bool = {
          "aws:SecureTransport"       = "false"
          "aws:PrincipalIsAWSService" = "false"
        }
      }
    }]
  })
}

# Private container-image repository.
resource "aws_ecr_repository" "release_tracker" {
  name                 = "release-tracker"
  image_tag_mutability = "IMMUTABLE"
  force_delete         = false

  encryption_configuration {
    encryption_type = "AES256"
  }

  image_scanning_configuration {
    scan_on_push = true
  }
}

output "state_bucket_name" {
  value = aws_s3_bucket.terraform_state.id
}

output "ecr_repository_url" {
  value = aws_ecr_repository.release_tracker.repository_url
}
