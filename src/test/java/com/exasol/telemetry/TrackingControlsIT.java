package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TrackingControlsIT {
    private static final String PROJECT_TAG = "projectTag";
    private static final String VERSION = "1.2.3";
    private static final String FEATURE = "myFeature";

    // [itest~tracking-controls-disable-env~1->scn~tracking-controls-disables-tracking-via-environment-variables~1]
    @Test
    void disablesTrackingViaEnvironmentVariables() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION)
                        .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "disabled")))
                        .build())) {
            client.track(FEATURE);

            Thread.sleep(150);
            assertThat(server.awaitRequests(1, Duration.ofMillis(150)), empty());
        }
    }

    // [itest~tracking-controls-disable-ci~1->scn~tracking-controls-disables-tracking-automatically-in-ci~1]
    @Test
    void disablesTrackingAutomaticallyWhenCiIsNonEmpty() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION)
                        .environment(new MapEnvironment(Map.of(TelemetryConfig.CI_ENV, "github-actions")))
                        .build())) {
            client.track(FEATURE);

            Thread.sleep(150);
            assertThat(server.awaitRequests(1, Duration.ofMillis(150)), empty());
        }
    }

    // [itest~tracking-controls-endpoint-override~1->scn~tracking-controls-overrides-configured-endpoint-via-environment-variable~1]
    @Test
    void overridesConfiguredEndpointViaEnvironmentVariable() throws Exception {
        final List<RecordingHttpServer.RecordedRequest> requests;
        try (RecordingHttpServer configuredServer = RecordingHttpServer.createSuccessServer();
                RecordingHttpServer overrideServer = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(configuredServer.configBuilder(PROJECT_TAG, VERSION)
                        .environment(new MapEnvironment(Map.of(TelemetryConfig.ENDPOINT_ENV, overrideServer.endpoint().toString())))
                        .build())) {
            client.track(FEATURE);

            requests = overrideServer.awaitRequests(1, Duration.ofSeconds(2));
            assertThat(requests, hasSize(1));
            assertThat(configuredServer.awaitRequests(1, Duration.ofMillis(150)), empty());
        }

        assertThat(requests.get(0).body(), containsString("\"category\":\"" + PROJECT_TAG + "\""));
        assertThat(requests.get(0).body(), containsString("\"productVersion\":\"" + VERSION + "\""));
        assertThat(requests.get(0).body(), containsString("\"version\":\"0.2.0\""));
    }
}
