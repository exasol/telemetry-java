package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class ShutdownFlushIT {
    @Test
    void flushesPendingEventsOnClose() throws Exception {
        List<RecordingHttpServer.RecordedRequest> requests;
        try (RecordingHttpServer server = RecordingHttpServer.createDelayedSuccessServer(150)) {
            final TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                    .retryTimeout(Duration.ofSeconds(1))
                    .build());
            client.track("checkout-started");

            client.close();
            requests = server.awaitRequests(1, Duration.ofSeconds(1));
        }

        assertEquals(1, requests.size());
        assertTrue(requests.get(0).body().contains("\"features\":{\"shop-ui.checkout-started\":["));
    }

    @Test
    void stopsBackgroundThreadsAfterClose() throws Exception {
        TelemetryClient client;
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer()) {
            client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint()).build());
            client.track("checkout-started");
            client.close();
        }

        assertTrue(client.awaitStopped(Duration.ofSeconds(1)));
        assertFalse(client.isRunning());
    }
}
