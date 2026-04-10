package com.exasol.telemetry;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrackingControlsIT
{
    @Test
    void disablesTrackingViaEnvironmentVariables() throws Exception
    {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                        .environment(new MapTelemetryEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "true")))
                        .build())) {
            TrackingResult result = client.track("checkout-started");

            Thread.sleep(150);
            assertEquals(TrackingResult.DISABLED, result);
            assertEquals(0, server.awaitRequests(1, Duration.ofMillis(150)).size());
        }
    }

    @Test
    void disablesTrackingAutomaticallyWhenCiIsTrue() throws Exception
    {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                        .environment(new MapTelemetryEnvironment(Map.of(TelemetryConfig.CI_ENV, "true")))
                        .build())) {
            TrackingResult result = client.track("checkout-started");

            Thread.sleep(150);
            assertEquals(TrackingResult.DISABLED, result);
            assertEquals(0, server.awaitRequests(1, Duration.ofMillis(150)).size());
        }
    }

    @Test
    void overridesConfiguredEndpointViaEnvironmentVariable() throws Exception
    {
        try (RecordingHttpServer configuredServer = RecordingHttpServer.createSuccessServer();
                RecordingHttpServer overrideServer = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(configuredServer.endpoint())
                        .environment(new MapTelemetryEnvironment(Map.of(TelemetryConfig.ENDPOINT_ENV, overrideServer.endpoint().toString())))
                        .build())) {
            TrackingResult result = client.track("checkout-started");

            assertEquals(TrackingResult.ACCEPTED, result);
            assertEquals(1, overrideServer.awaitRequests(1, Duration.ofSeconds(2)).size());
            assertEquals(0, configuredServer.awaitRequests(1, Duration.ofMillis(150)).size());
        }
    }
}
