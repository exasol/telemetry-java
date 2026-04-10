package com.exasol.telemetry;

import nl.jqno.equalsverifier.EqualsVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TelemetryEventTest
{
    @Test
    void verifiesEqualsAndHashCode()
    {
        EqualsVerifier.forClass(TelemetryEvent.class).verify();
    }

    @Test
    void exposesFeatureAndTimestamp()
    {
        TelemetryEvent event = new TelemetryEvent("project.feature", 42L);

        assertEquals("project.feature", event.getFeature());
        assertEquals(42L, event.getTimestamp());
    }
}
