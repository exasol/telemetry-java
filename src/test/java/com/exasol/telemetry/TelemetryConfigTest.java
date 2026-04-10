package com.exasol.telemetry;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryConfigTest
{
    @Test
    void usesDefaultsAndConfiguredValues()
    {
        TelemetryConfig config = TelemetryConfig.builder("project")
                .build();

        assertEquals("project", config.getProjectTag());
        assertEquals(TelemetryConfig.DEFAULT_ENDPOINT, config.getEndpoint());
        assertEquals(256, config.getQueueCapacity());
        assertEquals(Duration.ofSeconds(5), config.getRetryTimeout());
        assertFalse(config.isTrackingDisabled());
    }

    @Test
    void usesEndpointOverrideAndDisableEnvironmentValues()
    {
        TelemetryConfig config = TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .environment(new MapTelemetryEnvironment(Map.of(
                        TelemetryConfig.ENDPOINT_ENV, "https://override.example.com",
                        TelemetryConfig.DISABLED_ENV, "disabled")))
                .build();

        assertEquals(URI.create("https://override.example.com"), config.getEndpoint());
        assertTrue(config.isTrackingDisabled());
    }

    @Test
    void disablesTrackingAutomaticallyInCi()
    {
        TelemetryConfig config = TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .environment(new MapTelemetryEnvironment(Map.of(TelemetryConfig.CI_ENV, "github-actions")))
                .build();

        assertTrue(config.isTrackingDisabled());
    }

    @Test
    void treatsAnyNonEmptyEnvironmentValueAsDisabled()
    {
        assertFalse(TelemetryConfig.isDisabled(null));
        assertFalse(TelemetryConfig.isDisabled("   "));
        assertTrue(TelemetryConfig.isDisabled("false"));
        assertTrue(TelemetryConfig.isDisabled("true"));
        assertTrue(TelemetryConfig.isDisabled("1"));
        assertTrue(TelemetryConfig.isDisabled("on"));
        assertTrue(TelemetryConfig.isDisabled("github-actions"));
    }

    @Test
    void rejectsBlankProjectTag()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> TelemetryConfig.builder("  ").build());
        assertTrue(exception.getMessage().contains("projectTag"));
    }

    @Test
    void usesDefaultEndpointWhenNoEndpointIsConfigured()
    {
        TelemetryConfig config = TelemetryConfig.builder("project").build();

        assertEquals(TelemetryConfig.DEFAULT_ENDPOINT, config.getEndpoint());
    }

    @Test
    void rejectsNonPositiveNumbersAndDurations()
    {
        assertThrows(IllegalArgumentException.class, () -> TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .queueCapacity(0)
                .build());
        assertThrows(IllegalArgumentException.class, () -> TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .retryTimeout(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .initialRetryDelay(Duration.ofMillis(-1))
                .build());
        assertThrows(IllegalArgumentException.class, () -> TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .maxRetryDelay(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .connectTimeout(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> TelemetryConfig.builder("project").endpoint(URI.create("https://example.com"))
                .requestTimeout(Duration.ZERO)
                .build());
    }
}
