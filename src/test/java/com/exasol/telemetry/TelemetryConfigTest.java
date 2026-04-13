package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exasol.telemetry.TelemetryConfig.Builder;

class TelemetryConfigTest {
    @Test
    void usesDefaultsAndConfiguredValues() {
        final TelemetryConfig config = TelemetryConfig.builder("project")
                .build();

        assertEquals("project", config.getProjectTag());
        assertEquals(TelemetryConfig.DEFAULT_ENDPOINT, config.getEndpoint());
        assertEquals(256, config.getQueueCapacity());
        assertEquals(Duration.ofSeconds(5), config.getRetryTimeout());
        assertFalse(config.isTrackingDisabled());
    }

    @Test
    void usesEndpointOverrideAndDisableEnvironmentValues() {
        final TelemetryConfig config = defaultBuilder()
                .environment(new MapEnvironment(Map.of(
                        TelemetryConfig.ENDPOINT_ENV, "https://override.example.com",
                        TelemetryConfig.DISABLED_ENV, "disabled")))
                .build();

        assertEquals(URI.create("https://override.example.com"), config.getEndpoint());
        assertTrue(config.isTrackingDisabled());
    }

    @Test
    void disablesTrackingAutomaticallyInCi() {
        final TelemetryConfig config = defaultBuilder()
                .environment(new MapEnvironment(Map.of(TelemetryConfig.CI_ENV, "github-actions")))
                .build();

        assertTrue(config.isTrackingDisabled());
    }

    @Test
    void treatsAnyNonEmptyEnvironmentValueAsDisabled() {
        assertFalse(TelemetryConfig.isDisabled(null));
        assertFalse(TelemetryConfig.isDisabled("   "));
        assertTrue(TelemetryConfig.isDisabled("false"));
        assertTrue(TelemetryConfig.isDisabled("true"));
        assertTrue(TelemetryConfig.isDisabled("1"));
        assertTrue(TelemetryConfig.isDisabled("on"));
        assertTrue(TelemetryConfig.isDisabled("github-actions"));
    }

    @Test
    void rejectsBlankProjectTag() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> TelemetryConfig.builder("  ").build());
        assertTrue(exception.getMessage().contains("projectTag"));
    }

    @Test
    void usesDefaultEndpointWhenNoEndpointIsConfigured() {
        final TelemetryConfig config = TelemetryConfig.builder("project").build();

        assertEquals(TelemetryConfig.DEFAULT_ENDPOINT, config.getEndpoint());
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
        return TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"));
    }
}
