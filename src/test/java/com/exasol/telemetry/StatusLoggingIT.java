package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class StatusLoggingIT {
    private static final Logger LOGGER = Logger.getLogger(TelemetryClient.class.getName());

    @Test
    void logsWhenTelemetryIsEnabled() throws Exception {
        try (LogCapture capture = new LogCapture(LOGGER);
                RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                        .environment(new MapTelemetryEnvironment(Map.of()))
                        .build())) {
            final LogRecord enabledRecord = capture.await(logRecord -> logRecord.getLevel() == Level.INFO
                    && logRecord.getMessage().contains("Telemetry is enabled"), Duration.ofSeconds(1));

            assertTrue(client.isRunning());
            assertTrue(enabledRecord.getMessage().contains("Set EXASOL_TELEMETRY_DISABLE to any non-empty value to disable telemetry."));
            assertTrue(enabledRecord.getMessage().contains("EXASOL_TELEMETRY_DISABLE=<unset>"));
            assertTrue(enabledRecord.getMessage().contains("CI=<unset>"));
        }
    }

    @Test
    void logsWhenTelemetryIsDisabledWithMechanism() throws Exception {
        try (LogCapture capture = new LogCapture(LOGGER);
                RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                        .environment(new MapTelemetryEnvironment(Map.of(TelemetryConfig.DISABLED_ENV, "disabled")))
                        .build())) {
            final TrackingResult result = client.track("checkout-started");
            final LogRecord envRecord = capture.await(record -> record.getLevel() == Level.INFO
                    && record.getMessage().contains("Telemetry is disabled via EXASOL_TELEMETRY_DISABLE='disabled'."), Duration.ofSeconds(1));

            assertEquals(TrackingResult.DISABLED, result);
            assertTrue(envRecord.getMessage().contains("EXASOL_TELEMETRY_DISABLE='disabled'"));
        }

        try (LogCapture capture = new LogCapture(LOGGER);
                RecordingHttpServer server = RecordingHttpServer.createSuccessServer();
                TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                        .environment(new MapTelemetryEnvironment(Map.of(TelemetryConfig.CI_ENV, "github-actions")))
                        .build())) {
            final LogRecord ciRecord = capture.await(record -> record.getLevel() == Level.INFO
                    && record.getMessage().contains("Telemetry is disabled via CI='github-actions'."), Duration.ofSeconds(1));

            assertEquals(TrackingResult.DISABLED, client.track("checkout-started"));
            assertTrue(ciRecord.getMessage().contains("CI='github-actions'"));
        }
    }

    @Test
    void logsSentMessageCount() throws Exception {
        try (LogCapture capture = new LogCapture(LOGGER);
                RecordingHttpServer server = RecordingHttpServer.createSuccessServer()) {
            final TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                    .environment(new MapTelemetryEnvironment(Map.of()))
                    .build());
            try {
                assertEquals(TrackingResult.ACCEPTED, client.track("checkout-started"));
                client.close();

                final LogRecord record = capture.await(logRecord -> logRecord.getLevel() == Level.FINE
                        && logRecord.getMessage().contains("Telemetry sent to the server with 1 event(s)."), Duration.ofSeconds(2));
                assertTrue(record.getMessage().contains("1 event(s)"));
            } finally {
                client.close();
            }
        }
    }

    @Test
    void logsWhenTelemetryStops() throws Exception {
        try (LogCapture capture = new LogCapture(LOGGER);
                RecordingHttpServer server = RecordingHttpServer.createSuccessServer()) {
            final TelemetryClient client = TelemetryClient.create(TelemetryConfig.builder("shop-ui").endpoint(server.endpoint())
                    .environment(new MapTelemetryEnvironment(Map.of()))
                    .build());
            try {
                client.close();

                final LogRecord record = capture.await(logRecord -> logRecord.getLevel() == Level.FINE
                        && logRecord.getMessage().contains("Telemetry is stopped."), Duration.ofSeconds(1));
                assertEquals("Telemetry is stopped.", record.getMessage());
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

        private LogCapture(final Logger logger) {
            this.logger = logger;
            this.originalLevel = logger.getLevel();
            this.originalUseParentHandlers = logger.getUseParentHandlers();
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false);
            setLevel(Level.ALL);
            logger.addHandler(this);
        }

        @Override
        public void publish(final LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
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
                for (final LogRecord record : snapshot) {
                    if (predicate.test(record)) {
                        return record;
                    }
                }
                Thread.sleep(10);
            }
            throw new AssertionError("Expected log record not found. Captured: " + records);
        }
    }
}
