package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TelemetryClientTest {
    @Test
    void doesNotRunSenderWhenTrackingIsDisabled() throws Exception {
        final TelemetryConfig config = TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "true")))
                .build();

        final TelemetryClient client = TelemetryClient.create(config);
        try {
            client.track("feature");
            assertThat(client.awaitStopped(Duration.ofMillis(10)), is(true));
            assertThat(client.isRunning(), is(false));
        } finally {
            client.close();
        }
    }

    @Test
    void ignoresBlankFeatureName() {
        final TelemetryConfig config = TelemetryConfig.builder("project").endpoint(URI.create("https://example.com")).build();
        final TelemetryClient client = TelemetryClient.create(config);
        try {
            client.track(" ");
        } finally {
            client.close();
        }
    }

    @Test
    void ignoresTrackingAfterCloseAndCloseIsIdempotent() {
        final TelemetryConfig config = TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "true")))
                .build();
        final TelemetryClient client = TelemetryClient.create(config);

        client.close();
        client.close();

        client.track("feature");
    }
}
