package com.mallikraja.releasetracker.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReleaseStoreTest {
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private ReleaseStore store;

    @BeforeEach
    void setUp() {
        store = new ReleaseStore(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void startsEmpty() {
        assertThat(store.findAll(null)).isEmpty();
    }

    @Test
    void createsAndFindsARelease() {
        ReleaseRecord record = store.create("payments-api", "1.0.0-SNAPSHOT",
                DeploymentEnvironment.DEV, ReleaseStatus.DEPLOYED);
        assertThat(record.id()).isNotNull();
        assertThat(record.createdAt()).isEqualTo(NOW);
        assertThat(store.findById(record.id())).contains(record);
    }

    @Test
    void eachCreateGetsADifferentId() {
        ReleaseRecord first = store.create("payments-api", "1.0.0",
                DeploymentEnvironment.DEV, ReleaseStatus.PENDING);
        ReleaseRecord second = store.create("payments-api", "1.0.0",
                DeploymentEnvironment.DEV, ReleaseStatus.PENDING);
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(store.findAll(null)).hasSize(2);
    }

    @Test
    void filtersByEnvironment() {
        store.create("payments-api", "1.0.0", DeploymentEnvironment.DEV, ReleaseStatus.PENDING);
        ReleaseRecord qa = store.create("payments-api", "1.0.0", DeploymentEnvironment.QA, ReleaseStatus.DEPLOYED);
        assertThat(store.findAll(DeploymentEnvironment.QA)).containsExactly(qa);
    }

    @Test
    void deletesAnExistingRelease() {
        ReleaseRecord record = store.create("payments-api", "1.0.0",
                DeploymentEnvironment.DEV, ReleaseStatus.FAILED);
        assertThat(store.delete(record.id())).isTrue();
        assertThat(store.findById(record.id())).isEmpty();
        assertThat(store.delete(record.id())).isFalse();
    }

    @Test
    void missingIdReturnsEmpty() {
        assertThat(store.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void rejectsBlankNames() {
        assertThatThrownBy(() -> store.create(" ", "1.0.0", DeploymentEnvironment.DEV, ReleaseStatus.PENDING))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNewStoreDoesNotRetainRecords() {
        store.create("payments-api", "1.0.0", DeploymentEnvironment.DEV, ReleaseStatus.PENDING);
        assertThat(new ReleaseStore().findAll(null)).isEmpty();
    }
}
