package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class ShutdownFlushIT {
    private static final String PRODUCT_VERSION = "1.2.3";

    // [itest~shutdown-flush-pending-events~1->req~shutdown-flush~1]
    @Test
    void flushesPendingEventsOnClose() throws Exception {
        final List<RecordingHttpServer.RecordedRequest> requests;
        try (RecordingHttpServer server = RecordingHttpServer.createDelayedSuccessServer(150)) {
            final TelemetryClient client = TelemetryClient.create(server.configBuilder("shop-ui", PRODUCT_VERSION)
                    .retryTimeout(Duration.ofSeconds(1))
                    .build());
            client.track("checkout-started");

            client.close();
            requests = server.awaitRequests(1, Duration.ofSeconds(1));
        }

        assertThat(requests, hasSize(1));
        assertThat(requests.get(0).body(), containsString("\"category\":\"shop-ui\""));
        assertThat(requests.get(0).body(), containsString("\"version\":\"0.2.0\""));
        assertThat(requests.get(0).body(), containsString("\"productVersion\":\"1.2.3\""));
        assertThat(requests.get(0).body(), containsString("\"features\":{\"checkout-started\":["));
    }

    // [itest~shutdown-flush-stops-background-thread~1->req~shutdown-flush~1]
    @Test
    void stopsBackgroundThreadsAfterClose() throws Exception {
        final TelemetryClient client;
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer()) {
            client = TelemetryClient.create(server.configBuilder("shop-ui", PRODUCT_VERSION).build());
            client.track("checkout-started");
            client.close();
        }

        assertThat(client.awaitStopped(Duration.ofSeconds(1)), is(true));
        assertThat(client.isRunning(), is(false));
    }

    // [itest~shutdown-flush-respects-retry-timeout~1->req~shutdown-flush~1]
    @Test
    void respectsRetryTimeoutWhileFlushingOnClose() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createDelayedFlakyServer(Integer.MAX_VALUE, 1_000)) {
            final TelemetryClient client = TelemetryClient.create(server.configBuilder("shop-ui", PRODUCT_VERSION)
                    .retryTimeout(Duration.ofMillis(220))
                    .initialRetryDelay(Duration.ofMillis(20))
                    .maxRetryDelay(Duration.ofMillis(80))
                    .requestTimeout(Duration.ofSeconds(5))
                    .build());
            client.track("checkout-started");

            final Instant start = Instant.now();
            client.close();
            final long elapsedMillis = Duration.between(start, Instant.now()).toMillis();
            final int attempts = server.awaitRequests(1, Duration.ofSeconds(1)).size();

            assertThat("close should wait until the configured retry timeout is reached", elapsedMillis, greaterThanOrEqualTo(180L));
            assertThat("close should stop background flushing shortly after the retry timeout", elapsedMillis, lessThan(600L));
            assertThat("the sender should have started flushing before the timeout is reached", attempts, is(1));
        }
    }
}
