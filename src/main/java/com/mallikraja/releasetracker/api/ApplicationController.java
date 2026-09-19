package com.mallikraja.releasetracker.api;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationController {
    private final String version;
    private final String environment;

    public ApplicationController(@Value("${app.version}") String version,
                                 @Value("${app.environment}") String environment) {
        this.version = version;
        this.environment = environment;
    }

    @GetMapping("/")
    public Map<String, String> application() {
        return Map.of(
                "application", "release-tracker",
                "version", version,
                "environment", environment,
                "message", "Release Tracker API is running",
                "releases", "/api/releases",
                "health", "/actuator/health");
    }
}
