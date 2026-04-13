package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TrackingControlsIT {
    @Test
    void disablesTrackingViaEnvironmentVariables() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                        .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "disabled")))
                        .build())) {
            client.track("checkout-started");

            Thread.sleep(150);
            assertEquals(0, server.awaitRequests(1, Duration.ofMillis(150)).size());
        }
    }

    @Test
    void disablesTrackingAutomaticallyWhenCiIsNonEmpty() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                        .environment(new MapEnvironment(Map.of(TelemetryConfig.CI_ENV, "github-actions")))
                        .build())) {
            client.track("checkout-started");

            Thread.sleep(150);
            assertEquals(0, server.awaitRequests(1, Duration.ofMillis(150)).size());
        }
    }

    @Test
    void overridesConfiguredEndpointViaEnvironmentVariable() throws Exception {
        try (RecordingHttpServer configuredServer = RecordingHttpServer.createSuccessServer();
                RecordingHttpServer overrideServer = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(configuredServer.endpoint())
                        .environment(new MapEnvironment(Map.of(TelemetryConfig.ENDPOINT_ENV, overrideServer.endpoint().toString())))
                        .build())) {
            client.track("checkout-started");

            assertEquals(1, overrideServer.awaitRequests(1, Duration.ofSeconds(2)).size());
            assertEquals(0, configuredServer.awaitRequests(1, Duration.ofMillis(150)).size());
        }
    }
}
