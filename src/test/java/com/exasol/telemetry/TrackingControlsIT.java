package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TrackingControlsIT {
    private static final String PROJECT_TAG = "projectTag";
    private static final String FEATURE = "myFeature";

    @Test
    void disablesTrackingViaEnvironmentVariables() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG)
                        .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "disabled")))
                        .build())) {
            client.track(FEATURE);

            Thread.sleep(150);
            assertThat(server.awaitRequests(1, Duration.ofMillis(150)), empty());
        }
    }

    @Test
    void disablesTrackingAutomaticallyWhenCiIsNonEmpty() throws Exception {
        try (RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG)
                        .environment(new MapEnvironment(Map.of(TelemetryConfig.CI_ENV, "github-actions")))
                        .build())) {
            client.track(FEATURE);

            Thread.sleep(150);
            assertThat(server.awaitRequests(1, Duration.ofMillis(150)), empty());
        }
    }

    @Test
    void overridesConfiguredEndpointViaEnvironmentVariable() throws Exception {
        try (RecordingHttpServer configuredServer = RecordingHttpServer.createSuccessServer();
                RecordingHttpServer overrideServer = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(configuredServer.configBuilder(PROJECT_TAG)
                        .environment(new MapEnvironment(Map.of(TelemetryConfig.ENDPOINT_ENV, overrideServer.endpoint().toString())))
                        .build())) {
            client.track(FEATURE);

            assertThat(overrideServer.awaitRequests(1, Duration.ofSeconds(2)), hasSize(1));
            assertThat(configuredServer.awaitRequests(1, Duration.ofMillis(150)), empty());
        }
    }
}
