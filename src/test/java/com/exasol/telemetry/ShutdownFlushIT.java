package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.Duration;
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
}
