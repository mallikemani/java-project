package com.mallikraja.releasetracker.release;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateReleaseRequest(
        @NotBlank(message = "applicationName is required")
        @Size(max = 64, message = "applicationName must be at most 64 characters")
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*",
                message = "applicationName must start with a letter or number and contain only letters, numbers, dots, underscores or hyphens")
        String applicationName,

        @NotBlank(message = "version is required")
        @Size(max = 64, message = "version must be at most 64 characters")
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._+\\-]*",
                message = "version must contain only letters, numbers, dots, underscores, plus signs or hyphens")
        String version,

        @NotNull(message = "environment is required: DEV, QA or PROD")
        DeploymentEnvironment environment,

        @NotNull(message = "status is required: PENDING, DEPLOYED or FAILED")
        ReleaseStatus status) {
}
