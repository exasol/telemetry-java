package io.telemetryjava;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

public final class TelemetryConfig
{
    public static final String DISABLED_ENV = "TELEMETRY_JAVA_DISABLED";
    public static final String ENDPOINT_ENV = "TELEMETRY_JAVA_ENDPOINT";

    private final String projectTag;
    private final URI endpoint;
    private final int queueCapacity;
    private final Duration retryTimeout;
    private final Duration initialRetryDelay;
    private final Duration maxRetryDelay;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final boolean trackingDisabled;
    private final TelemetryEnvironment environment;

    private TelemetryConfig(Builder builder)
    {
        this.projectTag = requireText(builder.projectTag, "projectTag");
        this.environment = Objects.requireNonNull(builder.environment, "environment");
        this.endpoint = resolveEndpoint(builder.endpoint, environment);
        this.queueCapacity = positive(builder.queueCapacity, "queueCapacity");
        this.retryTimeout = positive(builder.retryTimeout, "retryTimeout");
        this.initialRetryDelay = positive(builder.initialRetryDelay, "initialRetryDelay");
        this.maxRetryDelay = positive(builder.maxRetryDelay, "maxRetryDelay");
        this.connectTimeout = positive(builder.connectTimeout, "connectTimeout");
        this.requestTimeout = positive(builder.requestTimeout, "requestTimeout");
        this.trackingDisabled = isDisabled(environment.getenv(DISABLED_ENV));
    }

    public static Builder builder(String projectTag, URI endpoint)
    {
        return new Builder(projectTag, endpoint);
    }

    public String getProjectTag()
    {
        return projectTag;
    }

    public URI getEndpoint()
    {
        return endpoint;
    }

    public int getQueueCapacity()
    {
        return queueCapacity;
    }

    public Duration getRetryTimeout()
    {
        return retryTimeout;
    }

    public Duration getInitialRetryDelay()
    {
        return initialRetryDelay;
    }

    public Duration getMaxRetryDelay()
    {
        return maxRetryDelay;
    }

    public Duration getConnectTimeout()
    {
        return connectTimeout;
    }

    public Duration getRequestTimeout()
    {
        return requestTimeout;
    }

    public boolean isTrackingDisabled()
    {
        return trackingDisabled;
    }

    static boolean isDisabled(String value)
    {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.equals("true") || normalized.equals("1") || normalized.equals("yes") || normalized.equals("on");
    }

    private static URI resolveEndpoint(URI configuredEndpoint, TelemetryEnvironment environment)
    {
        String override = environment.getenv(ENDPOINT_ENV);
        if (override != null && !override.trim().isEmpty()) {
            return URI.create(override.trim());
        }
        return Objects.requireNonNull(configuredEndpoint, "endpoint");
    }

    private static String requireText(String value, String field)
    {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static int positive(int value, String field)
    {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }

    private static Duration positive(Duration value, String field)
    {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }

    public static final class Builder
    {
        private final String projectTag;
        private final URI endpoint;
        private int queueCapacity = 256;
        private Duration retryTimeout = Duration.ofSeconds(5);
        private Duration initialRetryDelay = Duration.ofMillis(100);
        private Duration maxRetryDelay = Duration.ofSeconds(1);
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration requestTimeout = Duration.ofSeconds(2);
        private TelemetryEnvironment environment = TelemetryEnvironment.SystemEnvironment.INSTANCE;

        private Builder(String projectTag, URI endpoint)
        {
            this.projectTag = projectTag;
            this.endpoint = endpoint;
        }

        public Builder queueCapacity(int queueCapacity)
        {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Builder retryTimeout(Duration retryTimeout)
        {
            this.retryTimeout = retryTimeout;
            return this;
        }

        public Builder initialRetryDelay(Duration initialRetryDelay)
        {
            this.initialRetryDelay = initialRetryDelay;
            return this;
        }

        public Builder maxRetryDelay(Duration maxRetryDelay)
        {
            this.maxRetryDelay = maxRetryDelay;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout)
        {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout)
        {
            this.requestTimeout = requestTimeout;
            return this;
        }

        Builder environment(TelemetryEnvironment environment)
        {
            this.environment = environment;
            return this;
        }

        public TelemetryConfig build()
        {
            return new TelemetryConfig(this);
        }
    }
}
