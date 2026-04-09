package com.exasol.telemetry;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShutdownFlushIT
{
    @Test
    void flushesPendingEventsOnClose() throws Exception
    {
        List<RecordingHttpServer.RecordedRequest> requests;
        try (RecordingHttpServer server = RecordingHttpServer.createDelayedSuccessServer(150)) {
            TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui", server.endpoint())
                    .retryTimeout(Duration.ofSeconds(1))
                    .build());
            client.track("checkout-started");

            client.close();
            requests = server.awaitRequests(1, Duration.ofSeconds(1));
        }

        assertEquals(1, requests.size());
        assertTrue(requests.get(0).body().contains("\"feature\":\"checkout-started\""));
    }

    @Test
    void stopsBackgroundThreadsAfterClose() throws Exception
    {
        TelemetryClient client;
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer()) {
            client = TelemetryClient.create(TelemetryConfig.builder("shop-ui", server.endpoint()).build());
            client.track("checkout-started");
            client.close();
        }

        assertTrue(client.awaitStopped(Duration.ofSeconds(1)));
        assertFalse(client.isRunning());
    }
}
