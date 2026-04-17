package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class AsyncDeliveryIT {

    private static final String PROJECT_TAG = "projectTag";
    private static final String VERSION = "1.2.3";
    private static final String FEATURE = "myFeature";

    // [itest~async-delivery-over-http~1->scn~async-delivery-sends-queued-events-asynchronously-over-http~1]
    @Test
    void sendsQueuedEventsAsynchronouslyOverHttp() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createDelayedSuccessServer(300);
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION)
                        .retryTimeout(Duration.ofMillis(500))
                        .build())) {
            final long start = System.nanoTime();
            client.track(FEATURE);
            final long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

            final List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(1, Duration.ofSeconds(2));
            assertThat("track should return before the delayed HTTP request completes", elapsedMillis, lessThan(150L));
            assertThat(requests, hasSize(1));
        }
    }

    // [itest~async-delivery-retry-with-backoff~1->scn~async-delivery-retries-failed-delivery-with-exponential-backoff-until-timeout~1]
    @Test
    void retriesFailedDeliveryWithExponentialBackoffUntilTimeout() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createFlakyServer(2);
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION)
                        .retryTimeout(Duration.ofSeconds(1))
                        .initialRetryDelay(Duration.ofMillis(50))
                        .maxRetryDelay(Duration.ofMillis(200))
                        .build())) {
            client.track(FEATURE);
            final List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(3, Duration.ofSeconds(3));

            assertThat(requests, hasSize(3));
            assertThat(Duration.between(requests.get(0).receivedAt(), requests.get(1).receivedAt()).toMillis(), greaterThanOrEqualTo(40L));
            assertThat(Duration.between(requests.get(1).receivedAt(), requests.get(2).receivedAt()).toMillis(), greaterThanOrEqualTo(80L));
        }

        try (RecordingHttpServer server = RecordingHttpServer.createFlakyServer(Integer.MAX_VALUE)) {
            final TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION)
                    .retryTimeout(Duration.ofMillis(220))
                    .initialRetryDelay(Duration.ofMillis(20))
                    .maxRetryDelay(Duration.ofMillis(80))
                    .build());
            client.track(FEATURE);

            final Instant start = Instant.now();
            client.close();
            final long elapsedMillis = Duration.between(start, Instant.now()).toMillis();
            final int attempts = server.awaitRequests(2, Duration.ofSeconds(1)).size();

            assertThat("close should wait for retry timeout before giving up", elapsedMillis, greaterThanOrEqualTo(180L));
            assertThat("close should stop retrying once the timeout is reached", elapsedMillis, lessThan(1000L));
            assertThat("the sender should retry before timing out", attempts, greaterThanOrEqualTo(2));
        }
    }

    // [itest~async-delivery-batches-multiple-events-into-a-single-request~1->scn~async-delivery-batches-multiple-drained-events-into-a-single-protocol-message~1]
    @Test
    void batchesMultipleDrainedEventsIntoSingleProtocolMessage() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer()) {
            final TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION)
                    .retryTimeout(Duration.ofMillis(500))
                    .build());
            try {
            client.track("feature-a");
            client.track("feature-b");
            client.close();

            final List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(1, Duration.ofSeconds(2));
            assertThat(requests, hasSize(1));
            assertThat(requests.get(0).body(), containsString("\"feature-a\":["));
            assertThat(requests.get(0).body(), containsString("\"feature-b\":["));
            } finally {
                client.close();
            }
        }
    }
}
