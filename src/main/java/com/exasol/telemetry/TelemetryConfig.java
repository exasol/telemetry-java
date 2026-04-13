package com.exasol.telemetry;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

public final class TelemetryConfig {
    public static final String CI_ENV = "CI";
    public static final String DISABLED_ENV = "EXASOL_TELEMETRY_DISABLE";
    public static final String ENDPOINT_ENV = "EXASOL_TELEMETRY_ENDPOINT";
    public static final URI DEFAULT_ENDPOINT = URI.create("https://metrics.exasol.com");

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
        this.environment = Objects.requireNonNull(builder.environment, "environment");
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

    public static Builder builder(final String projectTag) {
        return new Builder(projectTag, null);
    }

    public String getProjectTag() {
        return projectTag;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public Duration getRetryTimeout() {
        return retryTimeout;
    }

    public Duration getInitialRetryDelay() {
        return initialRetryDelay;
    }

    public Duration getMaxRetryDelay() {
        return maxRetryDelay;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public boolean isTrackingDisabled() {
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

    static boolean isDisabled(final String value) {
        return value != null && !value.trim().isEmpty();
    }

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

        public Builder endpoint(final URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        Builder queueCapacity(final int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Builder retryTimeout(final Duration retryTimeout) {
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

        public TelemetryConfig build() {
            return new TelemetryConfig(this);
        }
    }
}
