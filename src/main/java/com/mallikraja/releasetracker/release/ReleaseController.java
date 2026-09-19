package com.mallikraja.releasetracker.release;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/releases")
public class ReleaseController {
    private static final Logger log = LoggerFactory.getLogger(ReleaseController.class);
    private final ReleaseStore store;

    public ReleaseController(ReleaseStore store) {
        this.store = store;
    }

    @PostMapping
    public ResponseEntity<ReleaseRecord> create(@Valid @RequestBody CreateReleaseRequest request) {
        ReleaseRecord record = store.create(request.applicationName(), request.version(),
                request.environment(), request.status());
        log.info("release_created id={} application={} version={} environment={} status={}",
                record.id(), record.applicationName(), record.version(), record.environment(), record.status());
        return ResponseEntity.created(URI.create("/api/releases/" + record.id())).body(record);
    }

    @GetMapping
    public List<ReleaseRecord> list(
            @RequestParam(name = "environment", required = false) DeploymentEnvironment environment) {
        return store.findAll(environment);
    }

    @GetMapping("/{id}")
    public ReleaseRecord get(@PathVariable("id") UUID id) {
        return store.findById(id).orElseThrow(() -> new ReleaseNotFoundException(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        if (!store.delete(id)) {
            throw new ReleaseNotFoundException(id);
        }
        log.info("release_deleted id={}", id);
        return ResponseEntity.noContent().build();
    }
}
