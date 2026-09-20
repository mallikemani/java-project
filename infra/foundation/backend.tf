terraform {
  backend "s3" {
    bucket = "java-project-tfstate-513594860053-us-west-2"
    key    = "foundation/terraform.tfstate"
    region = "us-west-2"

    encrypt             = true
    use_lockfile        = true
    allowed_account_ids = ["513594860053"]
  }
}
