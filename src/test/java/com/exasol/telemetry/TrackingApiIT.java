package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class TrackingApiIT {
    @Test
    void recordsTaggedFeatureUsageEvent() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder("shop-ui")
                        .retryTimeout(Duration.ofMillis(500))
                        .build())) {
            client.track("checkout-started");

            final List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(1, Duration.ofSeconds(2));
            assertThat(requests, hasSize(1));
            assertThat(requests.get(0).method(), is("POST"));
            assertThat(requests.get(0).body(), containsString("\"version\":\"0.1\""));
            assertThat(requests.get(0).body(), containsString("\"timestamp\":"));
            assertThat(requests.get(0).body(), containsString("\"features\":{\"shop-ui.checkout-started\":["));
        }
    }

    @Test
    void ignoresInvalidFeatureNames() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder("shop-ui").build())) {
            client.track(" ");

            Thread.sleep(150);
            assertThat(server.awaitRequests(1, Duration.ofMillis(150)), empty());
        }
    }
}
