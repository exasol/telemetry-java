package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exasol.telemetry.TelemetryConfig.Builder;

class TelemetryConfigTest {
    // [utest~telemetry-config-defaults~1->req~tracking-controls~1]
    @Test
    void usesDefaultsAndConfiguredValues() {
        final TelemetryConfig config = TelemetryConfig.builder("project", "1.2.3")
                .environment(MapEnvironment.empty())
                .build();

        assertThat(config.getProjectTag(), is("project"));
        assertThat(config.getProductVersion(), is("1.2.3"));
        assertThat(config.getEndpoint(), is(TelemetryConfig.DEFAULT_ENDPOINT));
        assertThat(config.getQueueCapacity(), is(256));
        assertThat(config.getRetryTimeout(), is(Duration.ofSeconds(5)));
        assertThat(config.isTrackingDisabled(), is(false));
    }

    // [utest~telemetry-config-client-identity-defaults~1->req~client-identity~1]
    @Test
    void usesDefaultsAndConfiguredValuesWithRealEnvironment() {
        final TelemetryConfig config = TelemetryConfig.builder("project", "1.2.3")
                .build();

        assertThat(config.getProjectTag(), is("project"));
        assertThat(config.getProductVersion(), is("1.2.3"));
        assertThat(config.getEndpoint(), is(TelemetryConfig.DEFAULT_ENDPOINT));
        assertThat(config.getQueueCapacity(), is(256));
        assertThat(config.getRetryTimeout(), is(Duration.ofSeconds(5)));
        // Can't verify isTrackingDisabled() because in CI the CI env variable is set
    }

    // [utest~telemetry-config-endpoint-override~1->req~tracking-controls~1]
    @Test
    void usesEndpointOverrideAndDisableEnvironmentValues() {
        final TelemetryConfig config = defaultBuilder()
                .environment(new MapEnvironment(Map.of(
                        TelemetryConfig.ENDPOINT_ENV, "https://override.example.com",
                        TelemetryConfig.DISABLED_ENV, "disabled")))
                .build();

        assertThat(config.getEndpoint(), is(URI.create("https://override.example.com")));
        assertThat(config.isTrackingDisabled(), is(true));
    }

    // [utest~telemetry-config-disable-in-ci~1->req~tracking-controls~1]
    @Test
    void disablesTrackingAutomaticallyInCi() {
        final TelemetryConfig config = defaultBuilder()
                .environment(new MapEnvironment(Map.of(TelemetryConfig.CI_ENV, "github-actions")))
                .build();

        assertThat(config.isTrackingDisabled(), is(true));
    }

    // [utest~telemetry-config-disabled-value-detection~1->req~tracking-controls~1]
    @Test
    void treatsAnyNonEmptyEnvironmentValueAsDisabled() {
        assertThat(TelemetryConfig.isDisabled(null), is(false));
        assertThat(TelemetryConfig.isDisabled("   "), is(false));
        assertThat(TelemetryConfig.isDisabled("false"), is(true));
        assertThat(TelemetryConfig.isDisabled("true"), is(true));
        assertThat(TelemetryConfig.isDisabled("1"), is(true));
        assertThat(TelemetryConfig.isDisabled("on"), is(true));
        assertThat(TelemetryConfig.isDisabled("github-actions"), is(true));
    }

    // [utest~telemetry-config-rejects-blank-project-tag~1->req~client-identity~1]
    @Test
    void rejectsBlankProjectTag() {
        final Builder builder = TelemetryConfig.builder("  ", "1.2.3");
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertThat(exception.getMessage(), containsString("projectTag"));
    }

    // [utest~telemetry-config-rejects-blank-product-version~1->req~client-identity~1]
    @Test
    void rejectsBlankProductVersion() {
        final Builder builder = TelemetryConfig.builder("project", " ");
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertThat(exception.getMessage(), containsString("productVersion"));
    }

    // [utest~telemetry-config-default-endpoint~1->req~tracking-controls~1]
    @Test
    void usesDefaultEndpointWhenNoEndpointIsConfigured() {
        final TelemetryConfig config = TelemetryConfig.builder("project", "1.2.3").build();
        assertThat(config.getEndpoint(), is(TelemetryConfig.DEFAULT_ENDPOINT));
    }

    @Test
    void rejectsNonPositiveNumbersAndDurations() {
        assertThrows(IllegalArgumentException.class, () -> defaultBuilder()
                .queueCapacity(0)
                .build());
        assertThrows(IllegalArgumentException.class, () -> defaultBuilder()
                .retryTimeout(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> defaultBuilder()
                .initialRetryDelay(Duration.ofMillis(-1))
                .build());
        assertThrows(IllegalArgumentException.class, () -> defaultBuilder()
                .maxRetryDelay(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> defaultBuilder()
                .connectTimeout(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> defaultBuilder()
                .requestTimeout(Duration.ZERO)
                .build());
    }

    private Builder defaultBuilder() {
        return TelemetryConfig.builder("project", "1.2.3").endpoint(URI.create("https://example.com"));
    }
}
