package com.exasol.telemetry;

import static java.util.Objects.requireNonNull;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
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
    private final Clock clock;
    private final boolean trackingEnabled;
    private final String featurePrefix;
    private final CountDownLatch terminated = new CountDownLatch(1);
    private volatile boolean closed;

    private TelemetryClient(final TelemetryConfig config) {
        this(config, Clock.systemUTC());
    }

    TelemetryClient(final TelemetryConfig config, final Clock clock) {
        this.config = requireNonNull(config, "config");
        this.clock = requireNonNull(clock, "clock");
        this.queue = new ArrayBlockingQueue<>(config.getQueueCapacity());
        this.transport = new HttpTransport(config);
        this.trackingEnabled = !config.isTrackingDisabled();
        this.featurePrefix = config.getProjectTag() + "~";
        this.senderThread = new Thread(this::runSender, "telemetry-java-sender");
        this.senderThread.setDaemon(true);
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
    // [impl~telemetry-client-create~1->req~tracking-api~1]
    public static TelemetryClient create(final TelemetryConfig config) {
        return new TelemetryClient(config);
    }

    /**
     * Queue a feature usage event for asynchronous delivery.
     *
     * @param feature feature name without the project tag prefix
     */
    // [impl~telemetry-client-track~1->req~tracking-api~1]
    public void track(final String feature) {
        if (!trackingEnabled || closed) {
            return;
        }

        final String sanitizedFeature = sanitizeText(feature);
        if (sanitizedFeature == null) {
            return;
        }

        final TelemetryEvent event = new TelemetryEvent(namespacedFeature(sanitizedFeature), clock.instant());
        enqueue(event);
    }

    @SuppressWarnings("java:S899") // Return value of offer ignored, this is fire-and-forget
    private void enqueue(final TelemetryEvent event) {
        queue.offer(event);
    }

    private String namespacedFeature(final String feature) {
        return featurePrefix + feature;
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

    // [impl~telemetry-client-send-with-retry~1->req~async-delivery~1]
    private void sendWithRetry(final List<TelemetryEvent> events) {
        final Instant start = clock.instant();
        final Message message = Message.fromEvents(start, events);
        final Instant deadline = start.plus(config.getRetryTimeout());
        Duration delay = config.getInitialRetryDelay();

        while (true) {
            try {
                transport.send(message);
                LOGGER.fine(() -> "Telemetry sent to the server with " + events.size() + " event(s).");
                return;
            } catch (final Exception exception) {
                LOGGER.fine(() -> "Telemetry sending failed for " + events.size() + " event(s): "
                        + rootCauseMessage(exception));
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
    // [impl~telemetry-client-close~1->req~shutdown-flush~1]
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
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
        LOGGER.info(() -> "Telemetry is enabled. Set " + TelemetryConfig.DISABLED_ENV + " to any non-empty value to disable telemetry. "
                + TelemetryConfig.DISABLED_ENV + "=" + formatEnvValue(config.getDisabledEnvValue()) + ", "
                + TelemetryConfig.CI_ENV + "=" + formatEnvValue(config.getCiEnvValue()) + ".");
    }

    private void logDisabled() {
        LOGGER.info(() -> "Telemetry is disabled via " + config.getDisableMechanism() + "="
                + formatEnvValue(config.getDisableMechanismValue()) + ".");
    }

    private static String formatEnvValue(final String value) {
        if (value == null) {
            return "<unset>";
        }
        return "'" + value + "'";
    }
}
