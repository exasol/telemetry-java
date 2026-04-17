package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.logging.*;

import org.junit.jupiter.api.Test;

class StatusLoggingIT {
    private static final String PROJECT_TAG = "projectTag";
    private static final String VERSION = "1.2.3";
    private static final String FEATURE = "myFeature";

    @Test
    // [itest~status-logging-enabled~1->scn~status-logging-logs-when-telemetry-is-enabled~1]
    void logsWhenTelemetryIsEnabled() throws Exception {
        try (LogCapture capture = new LogCapture();
                RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION).build())) {
            final LogRecord enabledRecord = capture.await(logRecord -> logRecord.getLevel() == Level.INFO
                    && logRecord.getMessage().contains("Telemetry is enabled"), Duration.ofSeconds(1));

            assertThat(client, instanceOf(AsyncTelemetryClient.class));
            assertThat(((AsyncTelemetryClient) client).isRunning(), is(true));
            assertThat(enabledRecord.getMessage(), containsString("Set EXASOL_TELEMETRY_DISABLE to any non-empty value to disable telemetry."));
            assertThat(enabledRecord.getMessage(), containsString("EXASOL_TELEMETRY_DISABLE=<unset>"));
            assertThat(enabledRecord.getMessage(), containsString("CI=<unset>"));
        }
    }

    @Test
    void logsWhenTelemetryIsDisabledViaHostConfiguration() throws Exception {
        try (LogCapture capture = new LogCapture();
                RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION)
                        .disableTracking()
                        .build())) {
            client.track(FEATURE);
            final LogRecord hostRecord = capture.await(logRecord -> logRecord.getLevel() == Level.INFO
                    && logRecord.getMessage().contains("Telemetry is disabled via host configuration."), Duration.ofSeconds(1));

            assertThat(hostRecord.getMessage(), is("Telemetry is disabled via host configuration."));
        }
    }

    @Test
    // [itest~status-logging-disabled~1->scn~status-logging-logs-when-telemetry-is-disabled~1]
    void logsWhenTelemetryIsDisabledViaEnvironmentVariable() throws Exception {
        try (LogCapture capture = new LogCapture();
                RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION)
                        .environment(new MapEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "disabled")))
                        .build())) {
            client.track(FEATURE);
            final LogRecord envRecord = capture.await(logRecord -> logRecord.getLevel() == Level.INFO
                    && logRecord.getMessage().contains("Telemetry is disabled via EXASOL_TELEMETRY_DISABLE='disabled'."), Duration.ofSeconds(1));

            assertThat(envRecord.getMessage(), containsString("EXASOL_TELEMETRY_DISABLE='disabled'"));
        }
    }

    @Test
    void logsWhenTelemetryIsDisabledViaCi() throws Exception {
        try (LogCapture capture = new LogCapture();
                RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION)
                        .environment(new MapEnvironment(Map.of(TelemetryConfig.CI_ENV, "github-actions")))
                        .build())) {
            final LogRecord ciRecord = capture.await(logRecord -> logRecord.getLevel() == Level.INFO
                    && logRecord.getMessage().contains("Telemetry is disabled via CI='github-actions'."), Duration.ofSeconds(1));

            client.track(FEATURE);
            assertThat(ciRecord.getMessage(), containsString("CI='github-actions'"));
        }
    }

    @Test
    // [itest~status-logging-send-count~1->scn~status-logging-logs-message-counts-when-telemetry-is-sent~1]
    void logsSentMessageCount() throws Exception {
        try (LogCapture capture = new LogCapture();
                RecordingHttpServer server = RecordingHttpServer.createSuccessServer()) {
            final TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION)
                    .build());
            try {
                client.track(FEATURE);
                client.close();

                final LogRecord logRecord = capture.await(r -> r.getLevel() == Level.FINE
                        && r.getMessage().contains("Telemetry sent to the server with 1 event(s)."), Duration.ofSeconds(2));
                assertThat(logRecord.getMessage(), containsString("1 event(s)"));
            } finally {
                client.close();
            }
        }
    }

    @Test
    // [itest~status-logging-send-failure~1->scn~status-logging-logs-when-telemetry-sending-fails~1]
    void logsWhenTelemetrySendingFails() throws Exception {
        try (LogCapture capture = new LogCapture();
                RecordingHttpServer server = RecordingHttpServer.createFlakyServer(1)) {
            final TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION)
                    .retryTimeout(Duration.ofMillis(500))
                    .initialRetryDelay(Duration.ofMillis(25))
                    .maxRetryDelay(Duration.ofMillis(25))
                    .build());
            try {
                client.track(FEATURE);
                client.close();

                final LogRecord logRecord = capture.await(r -> r.getLevel() == Level.FINE
                        && r.getMessage().contains("Telemetry sending failed"),
                        Duration.ofSeconds(2));
                assertThat(logRecord.getMessage(),
                        is("Telemetry sending failed for 1 event(s): server status 500 (telemetry rejected by test server)"));
            } finally {
                client.close();
            }
        }
    }

    @Test
    // [itest~status-logging-stopped~1->scn~status-logging-logs-when-telemetry-is-stopped~1]
    void logsWhenTelemetryStops() throws Exception {
        try (LogCapture capture = new LogCapture();
                RecordingHttpServer server = RecordingHttpServer.createSuccessServer()) {
            final TelemetryClient client = TelemetryClient.create(server.configBuilder(PROJECT_TAG, VERSION).build());
            try {
                client.close();
                final LogRecord logRecord = capture.await(r -> r.getLevel() == Level.FINE
                        && r.getMessage().contains("Telemetry is stopped."), Duration.ofSeconds(1));
                assertThat(logRecord.getMessage(), is("Telemetry is stopped."));
            } finally {
                client.close();
            }
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
