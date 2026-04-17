package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.logging.*;

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
            final LogRecord logRecord = capture.await(record -> record.getLevel() == Level.INFO
                    && record.getMessage().contains("Telemetry is enabled"), Duration.ofSeconds(1));

            assertThat(client, instanceOf(AsyncTelemetryClient.class));
            assertThat(logRecord.getMessage(), containsString("EXASOL_TELEMETRY_DISABLE=<unset>"));
            assertThat(logRecord.getMessage(), containsString("CI=<unset>"));
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
            final LogRecord logRecord = capture.await(record -> record.getLevel() == Level.INFO
                    && record.getMessage().contains("Telemetry is disabled"), Duration.ofSeconds(1));

            assertThat(client, instanceOf(NoOpTelemetryClient.class));
            assertThat(logRecord.getMessage(), containsString("EXASOL_TELEMETRY_DISABLE='disabled'"));
        }
    }

    private static final class LogCapture extends Handler implements AutoCloseable {
        private final Logger logger;
        private final CopyOnWriteArrayList<LogRecord> records = new CopyOnWriteArrayList<>();
        private final Level originalLevel;
        private final boolean originalUseParentHandlers;

        private LogCapture() {
            this.logger = Logger.getLogger("com.exasol.telemetry");
            this.originalLevel = logger.getLevel();
            this.originalUseParentHandlers = logger.getUseParentHandlers();
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false);
            setLevel(Level.ALL);
            logger.addHandler(this);
        }

        @Override
        public void publish(final LogRecord logRecord) {
            records.add(logRecord);
        }

        @Override
        public void flush() {
            // Nothing to do
        }

        @Override
        public void close() {
            logger.removeHandler(this);
            logger.setLevel(originalLevel);
            logger.setUseParentHandlers(originalUseParentHandlers);
        }

        private LogRecord await(final Predicate<LogRecord> predicate, final Duration timeout) throws InterruptedException {
            final Instant deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                final List<LogRecord> snapshot = new ArrayList<>(records);
                for (final LogRecord logRecord : snapshot) {
                    if (predicate.test(logRecord)) {
                        return logRecord;
                    }
                }
                Thread.sleep(10);
            }
            throw new AssertionError("Expected log record not found. Captured: " + records);
        }
    }
}
