package com.mallikraja.releasetracker.release;

import java.time.Instant;
import java.util.UUID;

/** Immutable representation returned by the HTTP API. */
public record ReleaseRecord(
        UUID id,
        String applicationName,
        String version,
        DeploymentEnvironment environment,
        ReleaseStatus status,
        Instant createdAt) {
}
