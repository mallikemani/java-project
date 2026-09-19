package com.mallikraja.releasetracker.release;

import java.util.UUID;

public class ReleaseNotFoundException extends RuntimeException {
    public ReleaseNotFoundException(UUID id) {
        super("Release " + id + " was not found");
    }
}
