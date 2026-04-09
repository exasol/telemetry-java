package io.telemetryjava;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackingApiIT
{
    @Test
    void recordsTaggedFeatureUsageEvent() throws Exception
    {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui", server.endpoint())
                        .retryTimeout(Duration.ofMillis(500))
                        .build())) {
            TrackingResult result = client.track("checkout-started", Map.of("screen", "basket"));

            List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(1, Duration.ofSeconds(2));
            assertEquals(TrackingResult.ACCEPTED, result);
            assertEquals(1, requests.size());
            assertEquals("POST", requests.get(0).method());
            assertTrue(requests.get(0).body().contains("\"projectTag\":\"shop-ui\""));
            assertTrue(requests.get(0).body().contains("\"feature\":\"checkout-started\""));
            assertTrue(requests.get(0).body().contains("\"screen\":\"basket\""));
        }
    }

    @Test
    void rejectsUnsupportedUsagePayloads() throws Exception
    {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui", server.endpoint()).build())) {
            TrackingResult result = client.track("checkout-started", Map.of("attempts", 3));

            Thread.sleep(150);
            assertEquals(TrackingResult.REJECTED, result);
            assertFalse(server.awaitRequests(1, Duration.ofMillis(150)).size() >= 1);
        }
    }
}
