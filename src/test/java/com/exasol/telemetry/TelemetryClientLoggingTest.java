package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

class TelemetryClientLoggingTest {
    // [utest~telemetry-client-logs-when-telemetry-is-enabled~1->scn~status-logging-logs-when-telemetry-is-enabled~1]
    @Test
    void logsWhenTelemetryIsEnabled() throws Exception {
        try (LogCapture capture = new LogCapture();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("project", "1.2.3")
                        .endpoint(URI.create("https://example.com"))
                        .environment(MapEnvironment.empty())
                        .build())) {
            final LogRecord logRecord = capture.await(r -> r.getLevel() == Level.INFO
                    && r.getMessage().contains("Telemetry is enabled"), Duration.ofSeconds(1));

            assertAll(
                    () -> assertThat(client, instanceOf(AsyncTelemetryClient.class)),
                    () -> assertThat(logRecord.getMessage(), containsString("EXASOL_TELEMETRY_DISABLE=<unset>")),
                    () -> assertThat(logRecord.getMessage(), containsString("CI=<unset>")));
        }
    }

    // [utest~telemetry-client-logs-when-telemetry-is-disabled~1->scn~status-logging-logs-when-telemetry-is-disabled~1]
    @Test
    void logsWhenTelemetryIsDisabled() throws Exception {
        try (LogCapture capture = new LogCapture();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("project", "1.2.3")
                        .endpoint(URI.create("https://example.com"))
                        .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "disabled")))
                        .build())) {
            final LogRecord logRecord = capture.await(r -> r.getLevel() == Level.INFO
                    && r.getMessage().contains("Telemetry is disabled"), Duration.ofSeconds(1));

            assertAll(
                    () -> assertThat(client, instanceOf(NoOpTelemetryClient.class)),
                    () -> assertThat(logRecord.getMessage(), containsString("EXASOL_TELEMETRY_DISABLE='disabled'")));
        }
    }
}
