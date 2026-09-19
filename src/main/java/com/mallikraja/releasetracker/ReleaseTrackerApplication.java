package com.mallikraja.releasetracker;

import com.mallikraja.releasetracker.release.ReleaseStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ReleaseTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReleaseTrackerApplication.class, args);
    }

    @Bean
    ReleaseStore releaseStore() {
        return new ReleaseStore();
    }
}
