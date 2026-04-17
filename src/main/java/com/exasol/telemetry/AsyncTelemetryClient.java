package com.exasol.telemetry;

import static java.util.Objects.requireNonNull;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

final class AsyncTelemetryClient implements TelemetryClient {
    private static final Logger LOGGER = Logger.getLogger(AsyncTelemetryClient.class.getName());

    private final TelemetryConfig config;
    private final BlockingQueue<TelemetryEvent> queue;
    private final HttpTransport transport;
    private final Thread senderThread;
    private final Clock clock;
    private final CountDownLatch terminated = new CountDownLatch(1);
    private volatile boolean closed;

    AsyncTelemetryClient(final TelemetryConfig config) {
        this(config, Clock.systemUTC());
    }

    AsyncTelemetryClient(final TelemetryConfig config, final Clock clock) {
        this.config = requireNonNull(config, "config");
        this.clock = requireNonNull(clock, "clock");
        this.queue = new ArrayBlockingQueue<>(config.getQueueCapacity());
        this.transport = new HttpTransport(config);
        this.senderThread = new Thread(this::runSender, "telemetry-java-sender");
        this.senderThread.setDaemon(true);
        this.senderThread.start();
    }

    @Override
    public void track(final String feature) {
        if (closed || feature == null) {
            return;
        }
        final TelemetryEvent event = new TelemetryEvent(feature, clock.instant());
        enqueue(event);
    }

    @SuppressWarnings("java:S899") // Intentionally ignore return value of offer() to avoid blocking the caller.
    private void enqueue(final TelemetryEvent event) {
        queue.offer(event);
    }

    private void runSender() {
        try {
            while (!closed || !queue.isEmpty()) {
                final TelemetryEvent event = queue.poll(100, TimeUnit.MILLISECONDS);
                if (event != null) {
                    sendWithRetry(drainBatch(event));
                }
            }
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            terminated.countDown();
        }
    }

    private List<TelemetryEvent> drainBatch(final TelemetryEvent firstEvent) {
        final List<TelemetryEvent> batch = new ArrayList<>();
        batch.add(firstEvent);
        queue.drainTo(batch);
        return batch;
    }

    // [impl~telemetry-client-send-with-retry~1->req~async-delivery~1]
    private void sendWithRetry(final List<TelemetryEvent> events) {
        final Instant start = clock.instant();
        final Message message = Message.fromEvents(config.getProjectTag(), config.getProductVersion(), start, events);
        final Instant deadline = start.plus(config.getRetryTimeout());
        Duration delay = config.getInitialRetryDelay();

        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            try {
                transport.send(message);
                // [impl~telemetry-client-log-send-count~1->req~status-logging~1]
                LOGGER.fine(() -> "Telemetry sent to the server with " + events.size() + " event(s).");
                return;
            } catch (final Exception exception) {
                // [impl~telemetry-client-log-send-failure~1->req~status-logging~1]
                LOGGER.fine(() -> "Telemetry sending failed for " + events.size() + " event(s): "
                        + rootCauseMessage(exception));
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                final Instant now = clock.instant();
                if (!now.isBefore(deadline)) {
                    return;
                }
                final Duration remaining = Duration.between(now, deadline);
                sleep(min(delay, remaining));
                delay = min(delay.multipliedBy(2), config.getMaxRetryDelay());
            }
        }
    }

    private static Duration min(final Duration left, final Duration right) {
        return left.compareTo(right) <= 0 ? left : right;
    }

    private static String rootCauseMessage(final Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof HttpException) {
                final HttpException httpException = (HttpException) cause;
                return "server status " + httpException.getStatusCode() + " (" + httpException.getServerStatus() + ")";
            }
            if (cause.getCause() == null) {
                final String message = cause.getMessage();
                if (message == null || message.isBlank()) {
                    return cause.getClass().getSimpleName();
                }
            }
            cause = cause.getCause();
        }
        return "";
    }

    private void sleep(final Duration duration) {
        try {
            Thread.sleep(Math.max(1, duration.toMillis()));
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    // Visible for testing
    boolean awaitStopped(final Duration timeout) throws InterruptedException {
        return terminated.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    // Visible for testing
    boolean isRunning() {
        return senderThread.isAlive();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        awaitSenderStop();
        // [impl~telemetry-client-log-stopped~1->req~status-logging~1]
        LOGGER.fine("Telemetry is stopped.");
    }

    private void awaitSenderStop() {
        final long timeoutNanos = config.getRetryTimeout().toNanos();
        final long deadlineNanos = System.nanoTime() + timeoutNanos;
        try {
            while (senderThread.isAlive()) {
                final long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0) {
                    senderThread.interrupt();
                    senderThread.join();
                    return;
                }
                TimeUnit.NANOSECONDS.timedJoin(senderThread, remainingNanos);
            }
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
