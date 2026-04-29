package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exasol.telemetry.TelemetryConfig.Builder;

class TelemetryConfigTest {
    private static final String PROJECT_SHORT_TAG = "project";
    private static final String VERSION = "1.2.3";

    @Test
    void usesDefaultsAndConfiguredValues() {
        final TelemetryConfig config = TelemetryConfig.builder(PROJECT_SHORT_TAG, VERSION)
                .environment(MapEnvironment.empty())
                .build();

        assertAll(
                () -> assertThat(config.getProjectTag(), is(PROJECT_SHORT_TAG)),
                () -> assertThat(config.getProductVersion(), is(VERSION)),
                () -> assertThat(config.getEndpoint(), is(TelemetryConfig.DEFAULT_ENDPOINT)),
                () -> assertThat(config.getQueueCapacity(), is(256)),
                () -> assertThat(config.getRetryTimeout(), is(Duration.ofSeconds(5))),
                () -> assertThat(config.isTrackingDisabled(), is(false)));
    }

    @Test
    void usesDefaultsAndConfiguredValuesWithRealEnvironment() {
        final TelemetryConfig config = TelemetryConfig.builder(PROJECT_SHORT_TAG, VERSION)
                .build();

        assertAll(
                () -> assertThat(config.getProjectTag(), is(PROJECT_SHORT_TAG)),
                () -> assertThat(config.getProductVersion(), is(VERSION)),
                () -> assertThat(config.getEndpoint(), is(TelemetryConfig.DEFAULT_ENDPOINT)),
                () -> assertThat(config.getQueueCapacity(), is(256)),
                () -> assertThat(config.getRetryTimeout(), is(Duration.ofSeconds(5))));
        // Can't verify isTrackingDisabled() because in CI the CI env variable is set
    }

    // [utest~telemetry-config-endpoint-override~1->scn~tracking-controls-overrides-the-configured-endpoint-via-environment-variable~1]
    // [utest~telemetry-config-disables-tracking-via-environment-variable~1->scn~tracking-controls-disables-tracking-via-environment-variables~1]
    @Test
    void usesEndpointOverrideAndDisableEnvironmentValues() {
        final TelemetryConfig config = defaultBuilder()
                .environment(new MapEnvironment(Map.of(
                        TelemetryConfig.ENDPOINT_ENV, "https://override.example.com",
                        TelemetryConfig.DISABLED_ENV, "disabled")))
                .build();

        assertAll(
                () -> assertThat(config.getEndpoint(), is(URI.create("https://override.example.com"))),
                () -> assertThat(config.isTrackingDisabled(), is(true)));
    }

    // [utest~telemetry-config-explicit-disable~1->scn~tracking-controls-disables-tracking-via-explicit-host-configuration~1]
    @Test
    void disablesTrackingExplicitlyInHostConfiguration() {
        final TelemetryConfig config = defaultBuilder()
                .disableTracking()
                .environment(MapEnvironment.empty())
                .build();

        assertAll(
                () -> assertThat(config.isTrackingDisabled(), is(true)),
                () -> assertThat(config.getDisableMechanism(), is("host configuration")),
                () -> assertThat(config.getDisableMechanismValue(), is(nullValue())));
    }

    @Test
    void prefersEnvironmentDisableMechanismOverHostConfiguration() {
        final TelemetryConfig config = defaultBuilder()
                .disableTracking()
                .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "disabled")))
                .build();

        assertAll(
                () -> assertThat(config.isTrackingDisabled(), is(true)),
                () -> assertThat(config.getDisableMechanism(), is(TelemetryConfig.DISABLED_ENV)),
                () -> assertThat(config.getDisableMechanismValue(), is("disabled")));
    }

    // [utest~telemetry-config-disable-in-ci~1->scn~tracking-controls-disables-tracking-automatically-in-ci~1]
    @Test
    void disablesTrackingAutomaticallyInCi() {
        final TelemetryConfig config = defaultBuilder()
                .environment(new MapEnvironment(Map.of(TelemetryConfig.CI_ENV, "github-actions")))
                .build();

        assertThat(config.isTrackingDisabled(), is(true));
    }

    @Test
    void treatsAnyNonEmptyEnvironmentValueAsDisabled() {
        assertAll(
                () -> assertThat(TelemetryConfig.isDisabled(null), is(false)),
                () -> assertThat(TelemetryConfig.isDisabled("   "), is(false)),
                () -> assertThat(TelemetryConfig.isDisabled("false"), is(true)),
                () -> assertThat(TelemetryConfig.isDisabled("true"), is(true)),
                () -> assertThat(TelemetryConfig.isDisabled("1"), is(true)),
                () -> assertThat(TelemetryConfig.isDisabled("on"), is(true)),
                () -> assertThat(TelemetryConfig.isDisabled("github-actions"), is(true)));
    }

    // [utest~telemetry-config-rejects-blank-project-tag~1->scn~client-identity-requires-project-tag-and-product-version-when-creating-telemetry-configuration~1]
    @Test
    void rejectsBlankProjectTag() {
        final Builder builder = TelemetryConfig.builder("  ", VERSION);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertThat(exception.getMessage(), containsString("projectTag"));
    }

    // [utest~telemetry-config-rejects-blank-product-version~1->scn~client-identity-requires-project-tag-and-product-version-when-creating-telemetry-configuration~1]
    @Test
    void rejectsBlankProductVersion() {
        final Builder builder = TelemetryConfig.builder(PROJECT_SHORT_TAG, " ");
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertThat(exception.getMessage(), containsString("productVersion"));
    }

    // [utest~telemetry-config-default-endpoint~1->scn~tracking-controls-uses-default-telemetry-endpoint~1]
    @Test
    void usesDefaultEndpointWhenNoEndpointIsConfigured() {
        final TelemetryConfig config = TelemetryConfig.builder(PROJECT_SHORT_TAG, VERSION)
                .environment(MapEnvironment.empty())
                .build();
        assertThat(config.getEndpoint(), is(URI.create("https://metrics.exasol.com/telemetry")));
    }

    @Test
    void rejectsNonPositiveQueueCapacity() {
        final Builder builder = defaultBuilder().queueCapacity(0);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void rejectsNonPositiveRetryTimeout() {
        final Builder builder = defaultBuilder().retryTimeout(Duration.ZERO);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void rejectsNegativeInitialRetryDelay() {
        final Builder builder = defaultBuilder().initialRetryDelay(Duration.ofMillis(-1));
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void rejectsNonPositiveMaxRetryDelay() {
        final Builder builder = defaultBuilder().maxRetryDelay(Duration.ZERO);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void rejectsNonPositiveConnectTimeout() {
        final Builder builder = defaultBuilder().connectTimeout(Duration.ZERO);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void rejectsNonPositiveRequestTimeout() {
        final Builder builder = defaultBuilder().requestTimeout(Duration.ZERO);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    private Builder defaultBuilder() {
        return TelemetryConfig.builder(PROJECT_SHORT_TAG, VERSION).endpoint(URI.create("https://example.com"));
    }
}
