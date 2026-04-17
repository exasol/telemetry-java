package com.exasol.telemetry;

import java.util.logging.Logger;

/**
 * Tracks feature usage events and delivers them asynchronously to the configured telemetry endpoint.
 * Create a client by building a {@link TelemetryConfig} with {@link TelemetryConfig#builder(String, String)} and passing it
 * to {@link #create(TelemetryConfig)}.
 */
public interface TelemetryClient extends AutoCloseable {
    /**
     * Create a telemetry client for the provided configuration.
     *
     * @param config telemetry runtime configuration
     * @return telemetry client
     */
    // [impl~tracking-api-disabled-call-event-work-no-op~1->scn~tracking-api-disabled-tracking-no-op-without-overhead~1]
    static TelemetryClient create(final TelemetryConfig config) {
        if (config.isTrackingDisabled()) {
            logDisabled(config);
            return new NoOpTelemetryClient();
        }
        logEnabled(config);
        return new AsyncTelemetryClient(config);
    }

    /**
     * Queue a feature usage event for asynchronous delivery.
     *
     * @param feature feature name provided by the caller
     */
    // [impl~tracking-api-enabled-event-delivery-prefix-productVersion-version-field-choices~1->scn~tracking-api-records-feature-usage-event~1]
    // [impl~tracking-api-after-close-client-call-delivery-event~1->scn~tracking-api-ignores-tracking-after-client-close~1]
    // [impl~tracking-api-enabled-null-name-call~1->scn~tracking-api-ignores-null-feature-names~1]
    void track(String feature);

    /**
     * Stop the client, send all remaining queued telemetry messages and release any resources that it owns.
     */
    @Override
    // [impl~shutdown-flush-client-completes-flushing-queued-events-on-close~1->scn~shutdown-flush-flushes-pending-events-on-close~1]
    void close();

    // [impl~status-logging-enabled-telemetry-CI-completes~1->scn~status-logging-logs-when-telemetry-is-enabled~1]
    private static void logEnabled(final TelemetryConfig config) {
        logger().info(() -> "Telemetry is enabled. Set " + TelemetryConfig.DISABLED_ENV + " to any non-empty value to disable telemetry. "
                + TelemetryConfig.DISABLED_ENV + "=" + formatEnvValue(config.getDisabledEnvValue()) + ", "
                + TelemetryConfig.CI_ENV + "=" + formatEnvValue(config.getCiEnvValue()) + ".");
    }

    // [impl~status-logging-disabled-disablement-CI-completes~1->scn~status-logging-logs-when-telemetry-is-disabled~1]
    private static void logDisabled(final TelemetryConfig config) {
        logger().info(() -> "Telemetry is disabled via " + config.getDisableMechanism() + "="
                + formatEnvValue(config.getDisableMechanismValue()) + ".");
    }

    // Create logger in private method to avoid a public static field in the interface.
    private static Logger logger() {
        return Logger.getLogger(TelemetryClient.class.getName());
    }

    private static String formatEnvValue(final String value) {
        if (value == null) {
            return "<unset>";
        }
        return "'" + value + "'";
    }
}
