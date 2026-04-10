package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.Test;

class HttpTelemetryTransportTest {
    @Test
    void sendsJsonPayloadToConfiguredClient() throws Exception {
        final CapturingRequestSender requestSender = new CapturingRequestSender(202);
        final HttpTelemetryTransport transport = new HttpTelemetryTransport(
                TelemetryConfig.builder("project").endpoint(URI.create("https://example.com")).build(),
                requestSender);

        transport.send(TelemetryMessage.fromEvents(List.of(new TelemetryEvent("project.feature", 10))));

        final HttpRequest request = requestSender.request;
        assertEquals("POST", request.method());
        assertEquals(URI.create("https://example.com"), request.uri());
        assertEquals("application/json", request.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(bodyToString(request).contains("\"features\":{\"project.feature\":[10]}"));
    }

    @Test
    void rejectsNonSuccessStatusCodes() throws Exception {
        final HttpTelemetryTransport transport = new HttpTelemetryTransport(
                TelemetryConfig.builder("project").endpoint(URI.create("https://example.com")).build(),
                request -> 500);

        final IOException exception = assertThrows(IOException.class,
                () -> transport.send(TelemetryMessage.fromEvents(List.of(new TelemetryEvent("project.feature", 10)))));
        assertTrue(exception.getMessage().contains("Unexpected response status"));
    }

    @Test
    void convertsInterruptedExceptionToIoException() throws Exception {
        final HttpTelemetryTransport transport = new HttpTelemetryTransport(
                TelemetryConfig.builder("project").endpoint(URI.create("https://example.com")).build(),
                request -> {
                    throw new InterruptedException("interrupted");
                });

        final IOException exception = assertThrows(IOException.class,
                () -> transport.send(TelemetryMessage.fromEvents(List.of(new TelemetryEvent("project.feature", 10)))));
        assertTrue(exception.getMessage().contains("Interrupted while sending telemetry"));
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
    }

    private static String bodyToString(final HttpRequest request) throws Exception {
        final HttpRequest.BodyPublisher publisher = request.bodyPublisher().orElseThrow();
        final CollectingSubscriber subscriber = new CollectingSubscriber();
        publisher.subscribe(subscriber);
        return subscriber.body();
    }

    private static final class CapturingRequestSender implements HttpTelemetryTransport.RequestSender {
        private final int statusCode;
        private HttpRequest request;

        private CapturingRequestSender(final int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public int send(final HttpRequest request) {
            this.request = request;
            return statusCode;
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
            final byte[] all = new byte[total];
            int offset = 0;
            for (final ByteBuffer buffer : buffers) {
                final ByteBuffer copy = buffer.asReadOnlyBuffer();
                final int length = copy.remaining();
                copy.get(all, offset, length);
                offset += length;
            }
            return new String(all, StandardCharsets.UTF_8);
        }
    }
}
