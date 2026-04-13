package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class TrackingApiIT {
    @Test
    void recordsTaggedFeatureUsageEvent() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                        .retryTimeout(Duration.ofMillis(500))
                        .build())) {
            client.track("checkout-started");

            final List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(1, Duration.ofSeconds(2));
            assertEquals(1, requests.size());
            assertEquals("POST", requests.get(0).method());
            assertTrue(requests.get(0).body().contains("\"version\":\"0.1\""));
            assertTrue(requests.get(0).body().contains("\"timestamp\":"));
            assertTrue(requests.get(0).body().contains("\"features\":{\"shop-ui.checkout-started\":["));
        }
    }

    @Test
    void ignoresInvalidFeatureNames() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint()).build())) {
            client.track(" ");

            Thread.sleep(150);
            assertFalse(server.awaitRequests(1, Duration.ofMillis(150)).size() >= 1);
        }
    }
}
