package com.mallikraja.releasetracker.release;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Phase 1: a thread-safe, process-local store. Data is lost on restart.
 * This is deliberately NOT a database or a multi-replica persistence solution.
 */
public final class ReleaseStore {
    private final ConcurrentMap<UUID, ReleaseRecord> releases = new ConcurrentHashMap<>();
    private final Clock clock;

    public ReleaseStore() {
        this(Clock.systemUTC());
    }

    public ReleaseStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ReleaseRecord create(String applicationName, String version,
                                DeploymentEnvironment environment, ReleaseStatus status) {
        requireNonBlank(applicationName, "applicationName");
        requireNonBlank(version, "version");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(status, "status");

        ReleaseRecord record;
        do {
            record = new ReleaseRecord(UUID.randomUUID(), applicationName, version,
                    environment, status, Instant.now(clock));
        } while (releases.putIfAbsent(record.id(), record) != null);
        return record;
    }

    public List<ReleaseRecord> findAll(DeploymentEnvironment environment) {
        return releases.values().stream()
                .filter(record -> environment == null || record.environment() == environment)
                .sorted(Comparator.comparing(ReleaseRecord::createdAt).reversed()
                        .thenComparing(ReleaseRecord::id))
                .toList();
    }

    public Optional<ReleaseRecord> findById(UUID id) {
        return Optional.ofNullable(releases.get(Objects.requireNonNull(id, "id")));
    }

    public boolean delete(UUID id) {
        return releases.remove(Objects.requireNonNull(id, "id")) != null;
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
