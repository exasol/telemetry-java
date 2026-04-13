package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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

        assertThat(event.getFeature(), is("project.feature"));
        assertThat(event.getTimestamp(), is(timestamp));
    }
}
