package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.*;
import java.util.List;
import java.util.Map;

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
            assertThat(requests.get(0).body(), containsString("\"features\":{\"shop-ui~checkout-started\":["));
        }
    }

    @Test
    void emitsPayloadAsValidJson() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder("shop-ui")
                        .retryTimeout(Duration.ofMillis(500))
                        .build())) {
            client.track("checkout-started");

            final List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(1, Duration.ofSeconds(2));
            assertThat(requests, hasSize(1));
            assertThat(JsonTestHelper.parseJson(requests.get(0).body()).containsKey("features"), is(true));
        }
    }

    @Test
    void keepsCallerThreadOverheadLowForAcceptedTracking() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createDelayedSuccessServer(300);
                TelemetryClient client = TelemetryClient.create(server.configBuilder("shop-ui")
                        .retryTimeout(Duration.ofMillis(500))
                        .build())) {
            final long start = System.nanoTime();
            client.track("checkout-started");
            final long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

            assertThat("track should return before the delayed HTTP request completes", elapsedMillis, lessThan(150L));
            assertThat(server.awaitRequests(1, Duration.ofSeconds(2)), hasSize(1));
        }
    }

    @Test
    void makesDisabledTrackingNoOpWithoutTelemetryOverhead() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer()) {
            final TelemetryConfig config = server.configBuilder("shop-ui")
                    .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "disabled")))
                    .build();
            final TelemetryClient client = new TelemetryClient(config, new FailingClock());
            try {
                client.track("checkout-started");

                assertThat(client.awaitStopped(Duration.ofMillis(10)), is(true));
                assertThat(client.isRunning(), is(false));
                assertThat(server.awaitRequests(1, Duration.ofMillis(150)), empty());
            } finally {
                client.close();
            }
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

    private static final class FailingClock extends Clock {
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            throw new AssertionError("disabled tracking should not capture a timestamp");
        }
    }
}
