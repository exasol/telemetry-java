package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exasol.telemetry.TelemetryConfig.Builder;

class TelemetryConfigTest {
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

    // [utest~telemetry-config-endpoint-override~1->scn~tracking-controls-overrides-the-configured-endpoint-via-environment-variable~1]
    // [utest~telemetry-config-disables-tracking-via-environment-variable~1->scn~tracking-controls-disables-tracking-via-environment-variables~1]
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

    // [utest~telemetry-config-explicit-disable~1->scn~tracking-controls-disables-tracking-via-explicit-host-configuration~1]
    @Test
    void disablesTrackingExplicitlyInHostConfiguration() {
        final TelemetryConfig config = defaultBuilder()
                .disableTracking()
                .environment(MapEnvironment.empty())
                .build();

        assertThat(config.isTrackingDisabled(), is(true));
        assertThat(config.getDisableMechanism(), is("host configuration"));
        assertThat(config.getDisableMechanismValue(), is(nullValue()));
    }

    @Test
    void prefersEnvironmentDisableMechanismOverHostConfiguration() {
        final TelemetryConfig config = defaultBuilder()
                .disableTracking()
                .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "disabled")))
                .build();

        assertThat(config.isTrackingDisabled(), is(true));
        assertThat(config.getDisableMechanism(), is(TelemetryConfig.DISABLED_ENV));
        assertThat(config.getDisableMechanismValue(), is("disabled"));
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
        assertThat(TelemetryConfig.isDisabled(null), is(false));
        assertThat(TelemetryConfig.isDisabled("   "), is(false));
        assertThat(TelemetryConfig.isDisabled("false"), is(true));
        assertThat(TelemetryConfig.isDisabled("true"), is(true));
        assertThat(TelemetryConfig.isDisabled("1"), is(true));
        assertThat(TelemetryConfig.isDisabled("on"), is(true));
        assertThat(TelemetryConfig.isDisabled("github-actions"), is(true));
    }

    // [utest~telemetry-config-rejects-blank-project-tag~1->scn~client-identity-requires-project-tag-and-product-version-when-creating-telemetry-configuration~1]
    @Test
    void rejectsBlankProjectTag() {
        final Builder builder = TelemetryConfig.builder("  ", "1.2.3");
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertThat(exception.getMessage(), containsString("projectTag"));
    }

    // [utest~telemetry-config-rejects-blank-product-version~1->scn~client-identity-requires-project-tag-and-product-version-when-creating-telemetry-configuration~1]
    @Test
    void rejectsBlankProductVersion() {
        final Builder builder = TelemetryConfig.builder("project", " ");
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertThat(exception.getMessage(), containsString("productVersion"));
    }

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
