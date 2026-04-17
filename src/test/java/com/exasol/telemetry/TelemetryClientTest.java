package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TelemetryClientTest {
    // [utest~telemetry-client-disabled-tracking~1->req~tracking-controls~1]
    @Test
    void doesNotRunSenderWhenTrackingIsDisabled() {
        final TelemetryConfig config = TelemetryConfig.builder("project", "1.2.3").endpoint(URI.create("https://example.com"))
                .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "true")))
                .build();

        final TelemetryClient client = TelemetryClient.create(config);
        try {
            client.track("feature");
            assertThat(client, instanceOf(NoOpTelemetryClient.class));
        } finally {
            client.close();
        }
    }

    // [utest~telemetry-client-invalid-feature~1->req~tracking-api~1]
    @Test
    void ignoresNullFeatureName() {
        final TelemetryConfig config = TelemetryConfig.builder("project", "1.2.3").endpoint(URI.create("https://example.com")).build();
        final TelemetryClient client = TelemetryClient.create(config);
        try {
            client.track(null);
        } finally {
            client.close();
        }
    }

    // [utest~telemetry-client-after-close~1->req~tracking-api~1]
    @Test
    void ignoresTrackingAfterClose() {
        final TelemetryConfig config = TelemetryConfig.builder("project", "1.2.3").endpoint(URI.create("https://example.com"))
                .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "true")))
                .build();
        final TelemetryClient client = TelemetryClient.create(config);

        client.close();

        assertDoesNotThrow(() -> client.track("feature"));
    }

    // [utest~telemetry-client-close-idempotent~1->req~shutdown-flush~1]
    @Test
    void makesCloseIdempotent() {
        final TelemetryConfig config = TelemetryConfig.builder("project", "1.2.3").endpoint(URI.create("https://example.com"))
                .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "true")))
                .build();
        final TelemetryClient client = TelemetryClient.create(config);

        client.close();

        assertDoesNotThrow(client::close);
    }
}
