package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.Test;

class HttpTransportTest {
    private static final String DUMMY_ENDPOINT = "https://example.com";
    private static final String PROJECT_TAG = "projectTag";
    private static final String FEATURE = "projectTag.feature";

    // [utest~http-transport-sends-json-payload~1->req~async-delivery~1]
    @Test
    void sendsJsonPayloadToConfiguredClient() throws IOException {
        final CapturingRequestSender requestSender = new CapturingRequestSender(202);
        final HttpTransport transport = new HttpTransport(
                TelemetryConfig.builder(PROJECT_TAG).endpoint(URI.create(DUMMY_ENDPOINT)).build(),
                requestSender);

        transport.send(Message.fromEvents(Instant.ofEpochMilli(30), List.of(new TelemetryEvent(FEATURE, Instant.ofEpochSecond(10)))));

        final HttpRequest request = requestSender.request;
        assertThat(request.method(), is("POST"));
        assertThat(request.uri(), is(URI.create(DUMMY_ENDPOINT)));
        assertThat(request.headers().firstValue("Content-Type").orElseThrow(), is("application/json"));
        assertThat(bodyToString(request), containsString("\"features\":{\"projectTag.feature\":[10]}"));
    }

    // [utest~http-transport-rejects-non-success~1->req~async-delivery~1]
    @Test
    void rejectsNonSuccessStatusCodes() {
        final HttpTransport transport = new HttpTransport(
                TelemetryConfig.builder(PROJECT_TAG).endpoint(URI.create(DUMMY_ENDPOINT)).build(),
                request -> new HttpTransport.Response(500, "server says no"));

        final HttpException exception = assertThrows(HttpException.class,
                () -> transport.send(Message.fromEvents(Instant.ofEpochSecond(30), List.of(new TelemetryEvent(FEATURE, Instant.ofEpochSecond(10))))));
        assertThat(exception.getStatusCode(), is(500));
        assertThat(exception.getServerStatus(), is("server says no"));
        assertThat(exception.getMessage(), is("server says no"));
    }

    // [utest~http-transport-handles-interruption~1->req~async-delivery~1]
    @Test
    void convertsInterruptedExceptionToIoException() {
        final HttpTransport transport = new HttpTransport(
                TelemetryConfig.builder(PROJECT_TAG).endpoint(URI.create(DUMMY_ENDPOINT)).build(),
                request -> {
                    throw new InterruptedException("interrupted");
                });

        final IOException exception = assertThrows(IOException.class,
                () -> transport.send(Message.fromEvents(Instant.ofEpochSecond(30), List.of(new TelemetryEvent(FEATURE, Instant.ofEpochSecond(10))))));
        assertThat(exception.getMessage(), containsString("Interrupted while sending telemetry"));
        assertThat(Thread.currentThread().isInterrupted(), is(true));
        Thread.interrupted();
    }

    private static String bodyToString(final HttpRequest request) {
        final HttpRequest.BodyPublisher publisher = request.bodyPublisher().orElseThrow();
        final CollectingSubscriber subscriber = new CollectingSubscriber();
        publisher.subscribe(subscriber);
        return subscriber.body();
    }

    private static final class CapturingRequestSender implements HttpTransport.RequestSender {
        private final int statusCode;
        private HttpRequest request;

        private CapturingRequestSender(final int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public HttpTransport.Response send(final HttpRequest request) {
            this.request = request;
            return new HttpTransport.Response(statusCode, "");
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
