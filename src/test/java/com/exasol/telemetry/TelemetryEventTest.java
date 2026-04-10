package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class TelemetryEventTest {
    @Test
    void verifiesEqualsAndHashCode() {
        EqualsVerifier.forClass(TelemetryEvent.class).verify();
    }

    @Test
    void exposesFeatureAndTimestamp() {
        final TelemetryEvent event = new TelemetryEvent("project.feature", 42L);

        assertEquals("project.feature", event.getFeature());
        assertEquals(42L, event.getTimestamp());
    }
}
