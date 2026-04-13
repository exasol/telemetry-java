package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TelemetryClientTest {
    @Test
    void doesNotRunSenderWhenTrackingIsDisabled() throws Exception {
        final TelemetryConfig config = TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .environment(new MapTelemetryEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "true")))
                .build();

        final TelemetryClient client = TelemetryClient.create(config);
        try {
            client.track("feature");
            assertTrue(client.awaitStopped(Duration.ofMillis(10)));
            assertFalse(client.isRunning());
        } finally {
            client.close();
        }
    }

    @Test
    void ignoresBlankFeatureName() {
        final TelemetryConfig config = TelemetryConfig.builder("project").endpoint(URI.create("https://example.com")).build();
        final TelemetryClient client = TelemetryClient.create(config);
        try {
            assertDoesNotThrow(() -> client.track(" "));
        } finally {
            client.close();
        }
    }

    @Test
    void ignoresTrackingAfterCloseAndCloseIsIdempotent() {
        final TelemetryConfig config = TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .environment(new MapTelemetryEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "true")))
                .build();
        final TelemetryClient client = TelemetryClient.create(config);

        client.close();
        client.close();

        assertDoesNotThrow(() -> client.track("feature"));
    }
}
