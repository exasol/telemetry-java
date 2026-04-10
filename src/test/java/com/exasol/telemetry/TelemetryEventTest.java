package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class TelemetryEventTest {
    @Test
    void verifiesEqualsAndHashCode() {
        EqualsVerifier.forClass(TelemetryEvent.class).verify();
    }

    @Test
    void exposesFeatureAndTimestamp() {
        final Instant timestamp = Instant.ofEpochSecond(42);
        final TelemetryEvent event = new TelemetryEvent("project.feature", timestamp);

        assertEquals("project.feature", event.getFeature());
        assertEquals(timestamp, event.getTimestamp());
    }
}
