package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class AsyncDeliveryIT {

    @Test
    void sendsQueuedEventsAsynchronouslyOverHttp() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createDelayedSuccessServer(300);
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                        .retryTimeout(Duration.ofMillis(500))
                        .build())) {
            final long start = System.nanoTime();
            final TrackingResult result = client.track("checkout-started");
            final long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

            final List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(1, Duration.ofSeconds(2));
            assertEquals(TrackingResult.ACCEPTED, result);
            assertTrue(elapsedMillis < 150, "track should return before the delayed HTTP request completes");
            assertEquals(1, requests.size());
        }
    }

    @Test
    void retriesFailedDeliveryWithExponentialBackoffUntilTimeout() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createFlakyServer(2);
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                        .retryTimeout(Duration.ofSeconds(1))
                        .initialRetryDelay(Duration.ofMillis(50))
                        .maxRetryDelay(Duration.ofMillis(200))
                        .build())) {
            final TrackingResult result = client.track("checkout-started");
            final List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(3, Duration.ofSeconds(3));

            assertEquals(TrackingResult.ACCEPTED, result);
            assertEquals(3, requests.size());
            assertTrue(Duration.between(requests.get(0).receivedAt(), requests.get(1).receivedAt()).toMillis() >= 40);
            assertTrue(Duration.between(requests.get(1).receivedAt(), requests.get(2).receivedAt()).toMillis() >= 80);
        }

        try (RecordingHttpServer server = RecordingHttpServer.createFlakyServer(Integer.MAX_VALUE)) {
            final TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                    .retryTimeout(Duration.ofMillis(220))
                    .initialRetryDelay(Duration.ofMillis(20))
                    .maxRetryDelay(Duration.ofMillis(80))
                    .build());
            client.track("checkout-started");

            final Instant start = Instant.now();
            client.close();
            final long elapsedMillis = Duration.between(start, Instant.now()).toMillis();
            final int attempts = server.awaitRequests(2, Duration.ofSeconds(1)).size();

            assertTrue(elapsedMillis >= 180, "close should wait for retry timeout before giving up");
            assertTrue(elapsedMillis < 1000, "close should stop retrying once the timeout is reached");
            assertTrue(attempts >= 2, "the sender should retry before timing out");
        }
    }
}
