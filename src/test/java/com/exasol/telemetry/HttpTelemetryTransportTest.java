package com.exasol.telemetry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpTelemetryTransportTest
{
    @Test
    void sendsJsonPayloadToConfiguredClient() throws Exception
    {
        CapturingRequestSender requestSender = new CapturingRequestSender(202);
        HttpTelemetryTransport transport = new HttpTelemetryTransport(
                TelemetryConfig.builder("project").endpoint(URI.create("https://example.com")).build(),
                requestSender);

        transport.send(TelemetryMessage.fromEvents(List.of(new TelemetryEvent("project.feature", 10))));

        HttpRequest request = requestSender.request;
        assertEquals("POST", request.method());
        assertEquals(URI.create("https://example.com"), request.uri());
        assertEquals("application/json", request.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(bodyToString(request).contains("\"features\":{\"project.feature\":[10]}"));
    }

    @Test
    void rejectsNonSuccessStatusCodes() throws Exception
    {
        HttpTelemetryTransport transport = new HttpTelemetryTransport(
                TelemetryConfig.builder("project").endpoint(URI.create("https://example.com")).build(),
                request -> 500);

        IOException exception = assertThrows(IOException.class,
                () -> transport.send(TelemetryMessage.fromEvents(List.of(new TelemetryEvent("project.feature", 10)))));
        assertTrue(exception.getMessage().contains("Unexpected response status"));
    }

    @Test
    void convertsInterruptedExceptionToIoException() throws Exception
    {
        HttpTelemetryTransport transport = new HttpTelemetryTransport(
                TelemetryConfig.builder("project").endpoint(URI.create("https://example.com")).build(),
                request -> {
                    throw new InterruptedException("interrupted");
                });

        IOException exception = assertThrows(IOException.class,
                () -> transport.send(TelemetryMessage.fromEvents(List.of(new TelemetryEvent("project.feature", 10)))));
        assertTrue(exception.getMessage().contains("Interrupted while sending telemetry"));
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
    }

    private static String bodyToString(HttpRequest request) throws Exception
    {
        HttpRequest.BodyPublisher publisher = request.bodyPublisher().orElseThrow();
        CollectingSubscriber subscriber = new CollectingSubscriber();
        publisher.subscribe(subscriber);
        return subscriber.body();
    }

    private static final class CapturingRequestSender implements HttpTelemetryTransport.RequestSender
    {
        private final int statusCode;
        private HttpRequest request;

        private CapturingRequestSender(int statusCode)
        {
            this.statusCode = statusCode;
        }

        @Override
        public int send(HttpRequest request)
        {
            this.request = request;
            return statusCode;
        }
    }

    private static final class CollectingSubscriber implements Flow.Subscriber<ByteBuffer>
    {
        private final List<ByteBuffer> buffers = new ArrayList<>();
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription)
        {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer item)
        {
            buffers.add(item);
        }

        @Override
        public void onError(Throwable throwable)
        {
            throw new AssertionError(throwable);
        }

        @Override
        public void onComplete()
        {
            if (subscription != null) {
                subscription.cancel();
            }
        }

        private String body()
        {
            int total = buffers.stream().mapToInt(ByteBuffer::remaining).sum();
            byte[] all = new byte[total];
            int offset = 0;
            for (ByteBuffer buffer : buffers) {
                ByteBuffer copy = buffer.asReadOnlyBuffer();
                int length = copy.remaining();
                copy.get(all, offset, length);
                offset += length;
            }
            return new String(all, StandardCharsets.UTF_8);
        }
    }
}
