package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TrackingApiIT {
    @Test
    void recordsTaggedFeatureUsageEvent() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                        .retryTimeout(Duration.ofMillis(500))
                        .build())) {
            final TrackingResult result = client.track("checkout-started");

            final List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(1, Duration.ofSeconds(2));
            assertEquals(TrackingResult.ACCEPTED, result);
            assertEquals(1, requests.size());
            assertEquals("POST", requests.get(0).method());
            assertTrue(requests.get(0).body().contains("\"version\":\"0.1\""));
            assertTrue(requests.get(0).body().contains("\"timestamp\":"));
            assertTrue(requests.get(0).body().contains("\"features\":{\"shop-ui.checkout-started\":["));
        }
    }

    @Test
    void rejectsUnsupportedUsagePayloads() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint()).build())) {
            final TrackingResult result = client.track("checkout-started", Map.of("screen", "basket"));

            Thread.sleep(150);
            assertEquals(TrackingResult.REJECTED, result);
            assertFalse(server.awaitRequests(1, Duration.ofMillis(150)).size() >= 1);
        }
    }
}
