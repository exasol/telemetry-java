package com.exasol.telemetry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public final class TelemetryClient implements AutoCloseable {
    private final TelemetryConfig config;
    private final BlockingQueue<TelemetryEvent> queue;
    private final HttpTelemetryTransport transport;
    private final Thread senderThread;
    private final CountDownLatch terminated = new CountDownLatch(1);
    private volatile boolean closed;

    private TelemetryClient(final TelemetryConfig config) {
        this.config = config;
        this.queue = new ArrayBlockingQueue<>(config.getQueueCapacity());
        this.transport = new HttpTelemetryTransport(config);
        this.senderThread = new Thread(this::runSender, "telemetry-java-sender");
        this.senderThread.setDaemon(true);
        if (!config.isTrackingDisabled()) {
            this.senderThread.start();
        } else {
            terminated.countDown();
        }
    }

    public static TelemetryClient create(final TelemetryConfig config) {
        return new TelemetryClient(Objects.requireNonNull(config, "config"));
    }

    public TrackingResult track(final String feature) {
        return track(feature, Map.of());
    }

    public TrackingResult track(final String feature, final Map<String, ?> attributes) {
        if (closed) {
            return TrackingResult.CLOSED;
        }
        if (config.isTrackingDisabled()) {
            return TrackingResult.DISABLED;
        }

        final String sanitizedFeature = sanitizeText(feature);
        final Map<String, String> sanitizedAttributes = sanitizeAttributes(attributes);
        if (sanitizedFeature == null || sanitizedAttributes == null || !sanitizedAttributes.isEmpty()) {
            return TrackingResult.REJECTED;
        }

        final TelemetryEvent event = new TelemetryEvent(namespacedFeature(sanitizedFeature), Instant.now());
        return queue.offer(event) ? TrackingResult.ACCEPTED : TrackingResult.REJECTED;
    }

    private Map<String, String> sanitizeAttributes(final Map<String, ?> attributes) {
        if (attributes == null) {
            return Map.of();
        }

        final Map<String, String> sanitized = new LinkedHashMap<>();
        for (final Map.Entry<String, ?> entry : attributes.entrySet()) {
            final String key = sanitizeText(entry.getKey());
            if (key == null) {
                return null;
            }

            final Object value = entry.getValue();
            if (!(value instanceof String)) {
                return null;
            }

            final String sanitizedValue = sanitizeText((String) value);
            if (sanitizedValue == null) {
                return null;
            }
            sanitized.put(key, sanitizedValue);
        }
        return sanitized;
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
        final TelemetryMessage message = TelemetryMessage.fromEvents(events);
        final Instant deadline = Instant.now().plus(config.getRetryTimeout());
        Duration delay = config.getInitialRetryDelay();

        while (true) {
            try {
                transport.send(message);
                return;
            } catch (final Exception ignored) {
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

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (!config.isTrackingDisabled()) {
            try {
                senderThread.join();
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
