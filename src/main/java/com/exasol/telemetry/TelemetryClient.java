package com.exasol.telemetry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class TelemetryClient implements AutoCloseable
{
    private final TelemetryConfig config;
    private final BlockingQueue<TelemetryEvent> queue;
    private final HttpTelemetryTransport transport;
    private final Thread senderThread;
    private final CountDownLatch terminated = new CountDownLatch(1);
    private volatile boolean closed;

    private TelemetryClient(TelemetryConfig config)
    {
        this.config = config;
        this.queue = new ArrayBlockingQueue<>(config.getQueueCapacity());
        this.transport = new HttpTelemetryTransport(config);
        this.senderThread = new Thread(this::runSender, "telemetry-java-sender");
        this.senderThread.setDaemon(true);
        if (!config.isTrackingDisabled()) {
            this.senderThread.start();
        }
        else {
            terminated.countDown();
        }
    }

    public static TelemetryClient create(TelemetryConfig config)
    {
        return new TelemetryClient(Objects.requireNonNull(config, "config"));
    }

    public TrackingResult track(String feature)
    {
        return track(feature, Map.of());
    }

    public TrackingResult track(String feature, Map<String, ?> attributes)
    {
        if (closed) {
            return TrackingResult.CLOSED;
        }
        if (config.isTrackingDisabled()) {
            return TrackingResult.DISABLED;
        }

        String sanitizedFeature = sanitizeText(feature);
        Map<String, String> sanitizedAttributes = sanitizeAttributes(attributes);
        if (sanitizedFeature == null || sanitizedAttributes == null || !sanitizedAttributes.isEmpty()) {
            return TrackingResult.REJECTED;
        }

        TelemetryEvent event = new TelemetryEvent(namespacedFeature(sanitizedFeature), Instant.now().getEpochSecond());
        return queue.offer(event) ? TrackingResult.ACCEPTED : TrackingResult.REJECTED;
    }

    private Map<String, String> sanitizeAttributes(Map<String, ?> attributes)
    {
        if (attributes == null) {
            return Map.of();
        }

        Map<String, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : attributes.entrySet()) {
            String key = sanitizeText(entry.getKey());
            if (key == null) {
                return null;
            }

            Object value = entry.getValue();
            if (!(value instanceof String)) {
                return null;
            }

            String sanitizedValue = sanitizeText((String) value);
            if (sanitizedValue == null) {
                return null;
            }
            sanitized.put(key, sanitizedValue);
        }
        return sanitized;
    }

    private String namespacedFeature(String feature)
    {
        return config.getProjectTag() + "." + feature;
    }

    private String sanitizeText(String value)
    {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void runSender()
    {
        try {
            while (!closed || !queue.isEmpty()) {
                TelemetryEvent event = queue.poll(100, TimeUnit.MILLISECONDS);
                if (event != null) {
                    sendWithRetry(drainBatch(event));
                }
            }
        }
        catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        finally {
            terminated.countDown();
        }
    }

    private List<TelemetryEvent> drainBatch(TelemetryEvent firstEvent)
    {
        List<TelemetryEvent> batch = new ArrayList<>();
        batch.add(firstEvent);
        queue.drainTo(batch);
        return batch;
    }

    private void sendWithRetry(List<TelemetryEvent> events)
    {
        TelemetryMessage message = TelemetryMessage.fromEvents(events);
        Instant deadline = Instant.now().plus(config.getRetryTimeout());
        Duration delay = config.getInitialRetryDelay();

        while (true) {
            try {
                transport.send(message);
                return;
            }
            catch (Exception ignored) {
                Instant now = Instant.now();
                if (!now.isBefore(deadline)) {
                    return;
                }
                Duration remaining = Duration.between(now, deadline);
                sleep(min(delay, remaining));
                delay = min(delay.multipliedBy(2), config.getMaxRetryDelay());
            }
        }
    }

    private static Duration min(Duration left, Duration right)
    {
        return left.compareTo(right) <= 0 ? left : right;
    }

    private void sleep(Duration duration)
    {
        try {
            Thread.sleep(Math.max(1, duration.toMillis()));
        }
        catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    boolean awaitStopped(Duration timeout) throws InterruptedException
    {
        return terminated.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    boolean isRunning()
    {
        return senderThread.isAlive();
    }

    @Override
    public void close()
    {
        if (closed) {
            return;
        }
        closed = true;
        if (!config.isTrackingDisabled()) {
            try {
                senderThread.join();
            }
            catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
