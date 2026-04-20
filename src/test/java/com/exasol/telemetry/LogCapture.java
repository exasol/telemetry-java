package com.exasol.telemetry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.logging.*;

final class LogCapture extends Handler implements AutoCloseable {
    private final Logger logger;
    private final CopyOnWriteArrayList<LogRecord> records = new CopyOnWriteArrayList<>();
    private final Level originalLevel;
    private final boolean originalUseParentHandlers;

    LogCapture() {
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

    @SuppressWarnings("java:S2925") // Intentionally uses Thread.sleep() for waiting for log records to be captured
    LogRecord await(final Predicate<LogRecord> predicate, final Duration timeout) throws InterruptedException {
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
