package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.*;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TrackingApiIT {
    private static final String PROJECT_TAG = "shop-ui";
    private static final String PRODUCT_VERSION = "1.2.3";
    private static final String FEATURE = "checkout-started";

    // [itest~tracking-api-records-tagged-feature~1->req~tracking-api~1]
    // [itest~tracking-api-emits-client-identity~1->req~client-identity~1]
    @Test
    void recordsFeatureUsageEventWithCategoryProtocolVersionAndProductVersion() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, PRODUCT_VERSION)
                        .retryTimeout(Duration.ofMillis(500))
                        .build())) {
            client.track(FEATURE);

            final List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(1, Duration.ofSeconds(2));
            assertThat(requests, hasSize(1));
            assertThat(requests.get(0).method(), is("POST"));
            assertThat(requests.get(0).body(), containsString("\"category\":\"shop-ui\""));
            assertThat(requests.get(0).body(), containsString("\"version\":\"0.2.0\""));
            assertThat(requests.get(0).body(), containsString("\"productVersion\":\"1.2.3\""));
            assertThat(requests.get(0).body(), containsString("\"timestamp\":"));
            assertThat(requests.get(0).body(), containsString("\"features\":{\"checkout-started\":["));
        }
    }

    // [itest~tracking-api-valid-json-payload~1->req~tracking-api~1]
    @Test
    void emitsPayloadAsValidJson() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, PRODUCT_VERSION)
                        .retryTimeout(Duration.ofMillis(500))
                        .build())) {
            client.track(FEATURE);

            final List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(1, Duration.ofSeconds(2));
            assertThat(requests, hasSize(1));
            assertThat(JsonTestHelper.parseJson(requests.get(0).body()).containsKey("features"), is(true));
        }
    }

    // [itest~tracking-api-low-caller-thread-overhead~1->req~tracking-api~1]
    @Test
    void keepsCallerThreadOverheadLowForAcceptedTracking() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createDelayedSuccessServer(300);
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, PRODUCT_VERSION)
                        .retryTimeout(Duration.ofMillis(500))
                        .build())) {
            final long start = System.nanoTime();
            client.track(FEATURE);
            final long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

            assertThat("track should return before the delayed HTTP request completes", elapsedMillis, lessThan(150L));
            assertThat(server.awaitRequests(1, Duration.ofSeconds(2)), hasSize(1));
        }
    }

    // [itest~tracking-api-disabled-no-op~1->req~tracking-api~1]
    @Test
    void makesDisabledTrackingNoOpWithoutTelemetryOverhead() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer()) {
            final TelemetryConfig config = server.configBuilder(PROJECT_TAG, PRODUCT_VERSION)
                    .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "disabled")))
                    .build();

            try (TelemetryClient client = new TelemetryClient(config, new FailingClock())) {
                client.track(FEATURE);

                assertThat(client.awaitStopped(Duration.ofMillis(10)), is(true));
                assertThat(client.isRunning(), is(false));
                assertThat(server.awaitRequests(1, Duration.ofMillis(150)), empty());
            }
        }
    }

    // [itest~tracking-api-invalid-feature-name~1->req~tracking-api~1]
    @Test
    void recordsFeatureUsageEventWithoutPrefixingOrValidation() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, PRODUCT_VERSION)
                        .build())) {
            client.track(" feature ");

            final List<RecordingHttpServer.RecordedRequest> requests = server.awaitRequests(1, Duration.ofSeconds(2));
            assertThat(requests, hasSize(1));
            assertThat(requests.get(0).body(), containsString("\"features\":{\" feature \":"));
        }
    }

    // [itest~tracking-api-ignores-null-feature-name~1->req~tracking-api~1]
    @Test
    void ignoresNullFeatureNames() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, PRODUCT_VERSION)
                        .build())) {
            client.track(null);

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
