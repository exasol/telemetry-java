package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

class AsyncTelemetryClientTest {
    private static final String PROJECT_TAG = "projectTag";
    private static final String VERSION = "1.2.3";
    private static final String FEATURE = "feature";
    private static final URI ENDPOINT = URI.create("https://example.com");

    // [utest~async-telemetry-client-keeps-caller-thread-overhead-low~1->scn~tracking-api-keeps-caller-thread-overhead-low-for-accepted-tracking~1]
    @Test
    void keepsCallerThreadOverheadLowForAcceptedTracking() {
        final TelemetryConfig config = configBuilder().build();
        final RecordingRequestSender requestSender = new RecordingRequestSender(List.of(202), 300L, "");
        try (AsyncTelemetryClient client = new AsyncTelemetryClient(config, Clock.systemUTC(), new HttpTransport(config, requestSender))) {
            final long start = System.nanoTime();
            client.track(FEATURE);
            final long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

            assertThat(elapsedMillis, lessThan(150L));
        }
    }

    // [utest~async-telemetry-client-retries-failed-delivery-with-exponential-backoff~1->scn~async-delivery-retries-failed-delivery-with-exponential-backoff-until-timeout~1]
    @Test
    @SuppressWarnings("java:S2093") // Not using try-with-resources to be able to test close() in the middle of the test
    void retriesFailedDeliveryWithExponentialBackoffUntilSuccess() {
        final TelemetryConfig config = configBuilder()
                .retryTimeout(Duration.ofMillis(500))
                .initialRetryDelay(Duration.ofMillis(40))
                .maxRetryDelay(Duration.ofMillis(80))
                .build();
        final RecordingRequestSender requestSender = new RecordingRequestSender(List.of(500, 500, 202), 0L, "server says no");
        final AsyncTelemetryClient client = new AsyncTelemetryClient(config, Clock.systemUTC(), new HttpTransport(config, requestSender));
        try {
            client.track(FEATURE);
            client.close();

            assertAll(
                    () -> assertThat(requestSender.attempts(), is(3)),
                    () -> assertThat(Duration.between(requestSender.attemptAt(0), requestSender.attemptAt(1)).toMillis(),
                            greaterThanOrEqualTo(30L)),
                    () -> assertThat(Duration.between(requestSender.attemptAt(1), requestSender.attemptAt(2)).toMillis(),
                            greaterThanOrEqualTo(60L)));
        } finally {
            client.close();
        }
    }

    // [utest~async-telemetry-client-flushes-pending-events-on-close~1->scn~shutdown-flush-flushes-pending-events-on-close~1]
    @Test
    @SuppressWarnings("java:S2093") // Not using try-with-resources to be able to test close() in the middle of the test
    void flushesPendingEventsOnClose() {
        final TelemetryConfig config = configBuilder().retryTimeout(Duration.ofMillis(300)).build();
        final RecordingRequestSender requestSender = new RecordingRequestSender(List.of(202), 75L, "");
        final AsyncTelemetryClient client = new AsyncTelemetryClient(config, Clock.systemUTC(), new HttpTransport(config, requestSender));
        try {
            client.track(FEATURE);

            client.close();

            assertAll(
                    () -> assertThat(requestSender.attempts(), is(1)),
                    () -> assertThat(requestSender.body(0), containsString("\"features\":{\"feature\":[")));
        } finally {
            client.close();
        }
    }

    // [utest~async-telemetry-client-stops-background-threads-after-close~1->scn~shutdown-flush-stops-background-threads-after-close~1]
    @Test
    @SuppressWarnings("java:S2093") // Not using try-with-resources to be able to test close() in the middle of the test
    void stopsBackgroundThreadsAfterClose() {
        final TelemetryConfig config = configBuilder().build();
        final RecordingRequestSender requestSender = new RecordingRequestSender(List.of(202), 0L, "");
        final AsyncTelemetryClient client = new AsyncTelemetryClient(config, Clock.systemUTC(), new HttpTransport(config, requestSender));
        try {
            client.track(FEATURE);

            client.close();

            assertAll(
                    () -> assertThat(client.awaitStopped(Duration.ofSeconds(1)), is(true)),
                    () -> assertThat(client.isRunning(), is(false)));
        } finally {
            client.close();
        }
    }

    // [utest~async-telemetry-client-logs-send-count~1->scn~status-logging-logs-message-counts-when-telemetry-is-sent~1]
    @Test
    @SuppressWarnings("java:S2093") // Not using try-with-resources to be able to test close() in the middle of the test
    void logsMessageCountsWhenTelemetryIsSent() throws Exception {
        final TelemetryConfig config = configBuilder().build();
        final RecordingRequestSender requestSender = new RecordingRequestSender(List.of(202), 0L, "");
        try (LogCapture capture = new LogCapture()) {
            final AsyncTelemetryClient client = new AsyncTelemetryClient(config, Clock.systemUTC(), new HttpTransport(config, requestSender));
            try {
                client.track(FEATURE);
                client.close();

                final LogRecord logRecord = capture.await(r -> r.getLevel() == Level.FINE
                        && r.getMessage().contains("Telemetry sent to the server with 1 event(s)."), Duration.ofSeconds(1));
                assertThat(logRecord.getMessage(), containsString("1 event(s)"));
            } finally {
                client.close();
            }
        }
    }

    // [utest~async-telemetry-client-logs-send-failure~1->scn~status-logging-logs-when-telemetry-sending-fails~1]
    @Test
    @SuppressWarnings("java:S2093") // Not using try-with-resources to be able to test close() in the middle of the test
    void logsWhenTelemetrySendingFails() throws Exception {
        final TelemetryConfig config = configBuilder()
                .retryTimeout(Duration.ofMillis(50))
                .initialRetryDelay(Duration.ofMillis(25))
                .maxRetryDelay(Duration.ofMillis(25))
                .build();
        final RecordingRequestSender requestSender = new RecordingRequestSender(List.of(500, 500), 0L, "server says no");
        try (LogCapture capture = new LogCapture()) {
            final AsyncTelemetryClient client = new AsyncTelemetryClient(config, Clock.systemUTC(), new HttpTransport(config, requestSender));
            try {
                client.track(FEATURE);
                client.close();

                final LogRecord logRecord = capture.await(r -> r.getLevel() == Level.FINE
                        && r.getMessage().contains("Telemetry sending failed"), Duration.ofSeconds(1));
                assertThat(logRecord.getMessage(), containsString("server status 500 (server says no)"));
            } finally {
                client.close();
            }
        }
    }

    // [utest~async-telemetry-client-logs-stopped~1->scn~status-logging-logs-when-telemetry-is-stopped~1]
    @Test
    @SuppressWarnings("java:S2093") // Not using try-with-resources to be able to test close() in the middle of the test
    void logsWhenTelemetryIsStopped() {
        final TelemetryConfig config = configBuilder().build();
        final RecordingRequestSender requestSender = new RecordingRequestSender(List.of(202), 0L, "");
        try (LogCapture capture = new LogCapture()) {
            final AsyncTelemetryClient client = new AsyncTelemetryClient(config, Clock.systemUTC(), new HttpTransport(config, requestSender));
            try {
                client.close();

                assertDoesNotThrow(() -> capture.await(r -> r.getLevel() == Level.FINE && r.getMessage().equals("Telemetry is stopped."),
                        Duration.ofSeconds(1)));
            } finally {
                client.close();
            }
        }
    }

    private TelemetryConfig.Builder configBuilder() {
        return TelemetryConfig.builder(PROJECT_TAG, VERSION)
                .endpoint(ENDPOINT)
                .environment(MapEnvironment.empty());
    }

    private static final class RecordingRequestSender implements HttpTransport.RequestSender {
        private final List<Integer> statusCodes;
        private final long delayMillis;
        private final String responseBody;
        private final List<Instant> attempts = new CopyOnWriteArrayList<>();
        private final List<String> requestBodies = new CopyOnWriteArrayList<>();
        private final AtomicInteger position = new AtomicInteger();

        private RecordingRequestSender(final List<Integer> statusCodes, final long delayMillis, final String responseBody) {
            this.statusCodes = statusCodes;
            this.delayMillis = delayMillis;
            this.responseBody = responseBody;
        }

        @Override
        public HttpTransport.Response send(final HttpRequest request) throws InterruptedException {
            attempts.add(Instant.now());
            requestBodies.add(bodyToString(request));
            if (delayMillis > 0) {
                Thread.sleep(delayMillis);
            }
            final int index = Math.min(position.getAndIncrement(), statusCodes.size() - 1);
            return new HttpTransport.Response(statusCodes.get(index), responseBody);
        }

        private int attempts() {
            return attempts.size();
        }

        private Instant attemptAt(final int index) {
            return attempts.get(index);
        }

        private String body(final int index) {
            return requestBodies.get(index);
        }

        private static String bodyToString(final HttpRequest request) {
            final HttpRequest.BodyPublisher publisher = request.bodyPublisher().orElseThrow();
            final CollectingSubscriber subscriber = new CollectingSubscriber();
            publisher.subscribe(subscriber);
            return subscriber.body();
        }
    }

    private static final class CollectingSubscriber implements Flow.Subscriber<ByteBuffer> {
        private final List<ByteBuffer> buffers = new ArrayList<>();
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(final Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(final ByteBuffer item) {
            buffers.add(item);
        }

        @Override
        public void onError(final Throwable throwable) {
            throw new AssertionError(throwable);
        }

        @Override
        public void onComplete() {
            if (subscription != null) {
                subscription.cancel();
            }
        }

        private String body() {
            final int total = buffers.stream().mapToInt(ByteBuffer::remaining).sum();
            final ByteBuffer combined = ByteBuffer.allocate(total);
            for (final ByteBuffer buffer : buffers) {
                final ByteBuffer copy = buffer.asReadOnlyBuffer();
                combined.put(copy);
            }
            combined.flip();
            return StandardCharsets.UTF_8.decode(combined).toString();
        }
    }
}
