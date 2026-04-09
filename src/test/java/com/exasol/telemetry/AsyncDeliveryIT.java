package com.exasol.telemetry;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncDeliveryIT
{
    @Test
    void sendsQueuedEventsAsynchronouslyOverHttp() throws Exception
    {
        try (RecordingHttpServer server = RecordingHttpServer.createDelayedSuccessServer(300);
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui", server.endpoint())
                        .retryTimeout(Duration.ofMillis(500))
                        .build())) {
            long start = System.nanoTime();
            TrackingResult result = client.track("checkout-started");
            long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

            List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(1, Duration.ofSeconds(2));
            assertEquals(TrackingResult.ACCEPTED, result);
            assertTrue(elapsedMillis < 150, "track should return before the delayed HTTP request completes");
            assertEquals(1, requests.size());
        }
    }

    @Test
    void retriesFailedDeliveryWithExponentialBackoffUntilTimeout() throws Exception
    {
        try (RecordingHttpServer server = RecordingHttpServer.createFlakyServer(2);
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui", server.endpoint())
                        .retryTimeout(Duration.ofSeconds(1))
                        .initialRetryDelay(Duration.ofMillis(50))
                        .maxRetryDelay(Duration.ofMillis(200))
                        .build())) {
            TrackingResult result = client.track("checkout-started");
            List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(3, Duration.ofSeconds(3));

            assertEquals(TrackingResult.ACCEPTED, result);
            assertEquals(3, requests.size());
            assertTrue(Duration.between(requests.get(0).receivedAt(), requests.get(1).receivedAt()).toMillis() >= 40);
            assertTrue(Duration.between(requests.get(1).receivedAt(), requests.get(2).receivedAt()).toMillis() >= 80);
        }

        try (RecordingHttpServer server = RecordingHttpServer.createFlakyServer(Integer.MAX_VALUE)) {
            TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui", server.endpoint())
                    .retryTimeout(Duration.ofMillis(220))
                    .initialRetryDelay(Duration.ofMillis(20))
                    .maxRetryDelay(Duration.ofMillis(80))
                    .build());
            client.track("checkout-started");

            Instant start = Instant.now();
            client.close();
            long elapsedMillis = Duration.between(start, Instant.now()).toMillis();
            int attempts = server.awaitRequests(2, Duration.ofSeconds(1)).size();

            assertTrue(elapsedMillis >= 180, "close should wait for retry timeout before giving up");
            assertTrue(elapsedMillis < 1000, "close should stop retrying once the timeout is reached");
            assertTrue(attempts >= 2, "the sender should retry before timing out");
        }
    }
}
