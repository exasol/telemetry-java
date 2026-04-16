package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class ShutdownFlushIT {
    // [itest~shutdown-flush-pending-events~1->req~shutdown-flush~1]
    @Test
    void flushesPendingEventsOnClose() throws Exception {
        final List<RecordingHttpServer.RecordedRequest> requests;
        try (RecordingHttpServer server = RecordingHttpServer.createDelayedSuccessServer(150)) {
            final TelemetryClient client = TelemetryClient.create(server.configBuilder("shop-ui")
                    .retryTimeout(Duration.ofSeconds(1))
                    .build());
            client.track("checkout-started");

            client.close();
            requests = server.awaitRequests(1, Duration.ofSeconds(1));
        }

        assertThat(requests, hasSize(1));
        assertThat(requests.get(0).body(), containsString("\"features\":{\"shop-ui~checkout-started\":["));
    }

    // [itest~shutdown-flush-stops-background-thread~1->req~shutdown-flush~1]
    @Test
    void stopsBackgroundThreadsAfterClose() throws Exception {
        final TelemetryClient client;
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer()) {
            client = TelemetryClient.create(server.configBuilder("shop-ui").build());
            client.track("checkout-started");
            client.close();
        }

        assertThat(client.awaitStopped(Duration.ofSeconds(1)), is(true));
        assertThat(client.isRunning(), is(false));
    }
}
