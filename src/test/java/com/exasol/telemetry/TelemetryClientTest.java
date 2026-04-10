package com.exasol.telemetry;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryClientTest
{
    @Test
    void returnsDisabledAndDoesNotRunSenderWhenTrackingIsDisabled() throws Exception
    {
        TelemetryConfig config = TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .environment(new MapTelemetryEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "true")))
                .build();

        TelemetryClient client = TelemetryClient.create(config);
        try {
            assertEquals(TrackingResult.DISABLED, client.track("feature"));
            assertTrue(client.awaitStopped(Duration.ofMillis(10)));
            assertFalse(client.isRunning());
        }
        finally {
            client.close();
        }
    }

    @Test
    void returnsRejectedForBlankFeatureOrUnsupportedAttributes()
    {
        TelemetryConfig config = TelemetryConfig.builder("project").endpoint(URI.create("https://example.com")).build();
        TelemetryClient client = TelemetryClient.create(config);
        try {
            assertEquals(TrackingResult.REJECTED, client.track(" "));
            assertEquals(TrackingResult.REJECTED, client.track("feature", Map.of("screen", "main")));
            assertEquals(TrackingResult.REJECTED, client.track("feature", Map.of("screen", 1)));
            assertEquals(TrackingResult.REJECTED, client.track("feature", Map.of(" ", "value")));
        }
        finally {
            client.close();
        }
    }

    @Test
    void returnsClosedAfterCloseAndCloseIsIdempotent()
    {
        TelemetryConfig config = TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .environment(new MapTelemetryEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "true")))
                .build();
        TelemetryClient client = TelemetryClient.create(config);

        client.close();
        client.close();

        assertEquals(TrackingResult.CLOSED, client.track("feature"));
    }
}
