package com.exasol.telemetry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelemetryEventTest
{
    @Test
    void exposesFeatureAndTimestamp()
    {
        TelemetryEvent event = new TelemetryEvent("project.feature", 42L);

        assertEquals("project.feature", event.getFeature());
        assertEquals(42L, event.getTimestamp());
    }
}
