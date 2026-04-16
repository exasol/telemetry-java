package com.exasol.telemetry;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Stores the runtime settings that control telemetry delivery, retry behavior, and environment-based overrides.
 */
public final class TelemetryConfig {
    /**
     * Name of the environment variable used to detect CI environments and disable telemetry automatically.
     */
    static final String CI_ENV = "CI";
    /**
     * Name of the environment variable that disables telemetry when set to a non-empty value.
     */
    static final String DISABLED_ENV = "EXASOL_TELEMETRY_DISABLE";
    /**
     * Name of the environment variable that overrides the telemetry endpoint.
     */
    static final String ENDPOINT_ENV = "EXASOL_TELEMETRY_ENDPOINT";
    /**
     * Default endpoint that receives telemetry events when no override is configured.
     */
    static final URI DEFAULT_ENDPOINT = URI.create("https://metrics.exasol.com");

    private final String projectTag;
    private final URI endpoint;
    private final String disabledEnvValue;
    private final String ciEnvValue;
    private final int queueCapacity;
    private final Duration retryTimeout;
    private final Duration initialRetryDelay;
    private final Duration maxRetryDelay;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final boolean trackingDisabled;
    private final Environment environment;

    private TelemetryConfig(final Builder builder) {
        this.projectTag = requireText(builder.projectTag, "projectTag");
        this.environment = requireNonNull(builder.environment, "environment");
        this.disabledEnvValue = environment.getenv(DISABLED_ENV);
        this.ciEnvValue = environment.getenv(CI_ENV);
        this.endpoint = resolveEndpoint(builder.endpoint, environment);
        this.queueCapacity = positive(builder.queueCapacity, "queueCapacity");
        this.retryTimeout = positive(builder.retryTimeout, "retryTimeout");
        this.initialRetryDelay = positive(builder.initialRetryDelay, "initialRetryDelay");
        this.maxRetryDelay = positive(builder.maxRetryDelay, "maxRetryDelay");
        this.connectTimeout = positive(builder.connectTimeout, "connectTimeout");
        this.requestTimeout = positive(builder.requestTimeout, "requestTimeout");
        this.trackingDisabled = isDisabled(disabledEnvValue) || isDisabled(ciEnvValue);
    }

    /**
     * Creates a builder for a telemetry configuration bound to the given project tag.
     *
     * @param projectTag project identifier used as the prefix for emitted feature names
     * @return configuration builder
     */
    public static Builder builder(final String projectTag) {
        return new Builder(projectTag, null);
    }

    String getProjectTag() {
        return projectTag;
    }

    URI getEndpoint() {
        return endpoint;
    }

    int getQueueCapacity() {
        return queueCapacity;
    }

    Duration getRetryTimeout() {
        return retryTimeout;
    }

    Duration getInitialRetryDelay() {
        return initialRetryDelay;
    }

    Duration getMaxRetryDelay() {
        return maxRetryDelay;
    }

    Duration getConnectTimeout() {
        return connectTimeout;
    }

    Duration getRequestTimeout() {
        return requestTimeout;
    }

    boolean isTrackingDisabled() {
        return trackingDisabled;
    }

    String getDisabledEnvValue() {
        return disabledEnvValue;
    }

    String getCiEnvValue() {
        return ciEnvValue;
    }

    String getDisableMechanism() {
        if (isDisabled(disabledEnvValue)) {
            return DISABLED_ENV;
        }
        if (isDisabled(ciEnvValue)) {
            return CI_ENV;
        }
        return null;
    }

    String getDisableMechanismValue() {
        if (isDisabled(disabledEnvValue)) {
            return disabledEnvValue;
        }
        if (isDisabled(ciEnvValue)) {
            return ciEnvValue;
        }
        return null;
    }

    // [impl~telemetry-config-disable-detection~1->req~tracking-controls~1]
    static boolean isDisabled(final String value) {
        return value != null && !value.trim().isEmpty();
    }

    // [impl~telemetry-config-resolve-endpoint~1->req~tracking-controls~1]
    private static URI resolveEndpoint(final URI configuredEndpoint, final Environment environment) {
        final String override = environment.getenv(ENDPOINT_ENV);
        if (override != null && !override.trim().isEmpty()) {
            return URI.create(override.trim());
        }
        return configuredEndpoint != null ? configuredEndpoint : DEFAULT_ENDPOINT;
    }

    private static String requireText(final String value, final String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static int positive(final int value, final String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }

    private static Duration positive(final Duration value, final String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }

    /**
     * Builds validated {@link TelemetryConfig} instances with optional overrides for transport and retry settings.
     */
    public static final class Builder {
        private final String projectTag;
        private URI endpoint;
        private int queueCapacity = 256;
        private Duration retryTimeout = Duration.ofSeconds(5);
        private Duration initialRetryDelay = Duration.ofMillis(100);
        private Duration maxRetryDelay = Duration.ofSeconds(1);
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration requestTimeout = Duration.ofSeconds(2);
        private Environment environment = Environment.SystemEnvironment.INSTANCE;

        private Builder(final String projectTag, final URI endpoint) {
            this.projectTag = projectTag;
            this.endpoint = endpoint;
        }

        Builder endpoint(final URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        Builder queueCapacity(final int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        Builder retryTimeout(final Duration retryTimeout) {
            this.retryTimeout = retryTimeout;
            return this;
        }

        Builder initialRetryDelay(final Duration initialRetryDelay) {
            this.initialRetryDelay = initialRetryDelay;
            return this;
        }

        Builder maxRetryDelay(final Duration maxRetryDelay) {
            this.maxRetryDelay = maxRetryDelay;
            return this;
        }

        Builder connectTimeout(final Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        Builder requestTimeout(final Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        Builder environment(final Environment environment) {
            this.environment = environment;
            return this;
        }

        /**
         * Build a validated telemetry configuration instance.
         *
         * @return telemetry configuration
         */
        public TelemetryConfig build() {
            return new TelemetryConfig(this);
        }
    }
}
