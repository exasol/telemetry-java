package com.exasol.telemetry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Tracks feature usage events and delivers them asynchronously to the configured telemetry endpoint.
 * Create a client by building a {@link TelemetryConfig} with {@link TelemetryConfig#builder(String)} and passing it
 * to {@link #create(TelemetryConfig)}.
 */
public final class TelemetryClient implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(TelemetryClient.class.getName());

    private final TelemetryConfig config;
    private final BlockingQueue<TelemetryEvent> queue;
    private final HttpTransport transport;
    private final Thread senderThread;
    private final CountDownLatch terminated = new CountDownLatch(1);
    private volatile boolean closed;

    private TelemetryClient(final TelemetryConfig config) {
        this.config = config;
        this.queue = new ArrayBlockingQueue<>(config.getQueueCapacity());
        this.transport = new HttpTransport(config);
        this.senderThread = new Thread(this::runSender, "telemetry-java-sender");
        this.senderThread.setDaemon(true);
        final boolean trackingEnabled = !config.isTrackingDisabled();
        if (trackingEnabled) {
            this.senderThread.start();
            logEnabled();
        } else {
            terminated.countDown();
            logDisabled();
        }
    }

    /**
     * Create a telemetry client for the provided configuration.
     *
     * @param config telemetry runtime configuration
     * @return telemetry client
     */
    public static TelemetryClient create(final TelemetryConfig config) {
        return new TelemetryClient(Objects.requireNonNull(config, "config"));
    }

    /**
     * Queue a feature usage event for asynchronous delivery.
     *
     * @param feature feature name without the project tag prefix
     */
    public void track(final String feature) {
        if (closed) {
            return;
        }
        if (config.isTrackingDisabled()) {
            return;
        }

        final String sanitizedFeature = sanitizeText(feature);
        if (sanitizedFeature == null) {
            return;
        }

        // TODO: get clock injected
        final TelemetryEvent event = new TelemetryEvent(namespacedFeature(sanitizedFeature), Instant.now());
        enqueue(event);
    }

    @SuppressWarnings("java:S899") // Return value of offer ignored, this is fire-and-forget
    private void enqueue(final TelemetryEvent event) {
        queue.offer(event);
    }

    private String namespacedFeature(final String feature) {
        return config.getProjectTag() + "." + feature;
    }

    private String sanitizeText(final String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
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

    private void sendWithRetry(final List<TelemetryEvent> events) {
        final Message message = Message.fromEvents(events);
        // Todo: get clock injected
        final Instant deadline = Instant.now().plus(config.getRetryTimeout());
        Duration delay = config.getInitialRetryDelay();

        while (true) {
            try {
                transport.send(message);
                LOGGER.fine("Telemetry sent to the server with " + events.size() + " event(s).");
                return;
            } catch (final Exception exception) {
                LOGGER.fine("Telemetry sending failed for " + events.size() + " event(s): "
                        + rootCauseMessage(exception));
                final Instant now = Instant.now();
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

    boolean awaitStopped(final Duration timeout) throws InterruptedException {
        return terminated.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    boolean isRunning() {
        return senderThread.isAlive();
    }

    /**
     * Stop the sender thread and wait for any queued events to be flushed before returning.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        final boolean trackingEnabled = !config.isTrackingDisabled();
        if (trackingEnabled) {
            try {
                senderThread.join();
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.fine("Telemetry is stopped.");
    }

    private void logEnabled() {
        LOGGER.info("Telemetry is enabled. Set " + TelemetryConfig.DISABLED_ENV + " to any non-empty value to disable telemetry. "
                + TelemetryConfig.DISABLED_ENV + "=" + formatEnvValue(config.getDisabledEnvValue()) + ", "
                + TelemetryConfig.CI_ENV + "=" + formatEnvValue(config.getCiEnvValue()) + ".");
    }

    private void logDisabled() {
        LOGGER.info("Telemetry is disabled via " + config.getDisableMechanism() + "="
                + formatEnvValue(config.getDisableMechanismValue()) + ".");
    }

    private static String formatEnvValue(final String value) {
        if (value == null) {
            return "<unset>";
        }
        return "'" + value + "'";
    }
}
