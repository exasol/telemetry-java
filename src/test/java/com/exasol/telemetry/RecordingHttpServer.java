package com.exasol.telemetry;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

final class RecordingHttpServer implements AutoCloseable {
    private final HttpServer server;
    private final CopyOnWriteArrayList<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    private final int failuresBeforeSuccess;
    private final long responseDelayMillis;

    private RecordingHttpServer(final int failuresBeforeSuccess, final long responseDelayMillis) {
        try {
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
        this.failuresBeforeSuccess = failuresBeforeSuccess;
        this.responseDelayMillis = responseDelayMillis;
        this.server.createContext("/telemetry", this::handle);
        this.server.start();
    }

    static RecordingHttpServer createSuccessServer() {
        return new RecordingHttpServer(0, 0);
    }

    static RecordingHttpServer createDelayedSuccessServer(final long responseDelayMillis) {
        return new RecordingHttpServer(0, responseDelayMillis);
    }

    static RecordingHttpServer createFlakyServer(final int failuresBeforeSuccess) {
        return new RecordingHttpServer(failuresBeforeSuccess, 0);
    }

    URI endpoint() {
        return URI.create("http://localhost:" + server.getAddress().getPort() + "/telemetry");
    }

    List<RecordedRequest> awaitRequests(final int expectedCount, final Duration timeout) throws InterruptedException {
        final Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (requests.size() >= expectedCount) {
                return new ArrayList<>(requests);
            }
            Thread.sleep(10);
        }
        return new ArrayList<>(requests);
    }

    private void handle(final HttpExchange exchange) throws IOException {
        byte[] body;
        try (InputStream stream = exchange.getRequestBody()) {
            body = stream.readAllBytes();
        }

        requests.add(new RecordedRequest(exchange.getRequestMethod(), new String(body, StandardCharsets.UTF_8), Instant.now()));

        if (responseDelayMillis > 0) {
            try {
                Thread.sleep(responseDelayMillis);
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        final int status = requests.size() <= failuresBeforeSuccess ? 500 : 202;
        final byte[] responseBody = status >= 400 ? "telemetry rejected by test server".getBytes(StandardCharsets.UTF_8) : new byte[0];
        exchange.sendResponseHeaders(status, responseBody.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(responseBody);
        }
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    static final class RecordedRequest {
        private final String method;
        private final String body;
        private final Instant receivedAt;

        private RecordedRequest(final String method, final String body, final Instant receivedAt) {
            this.method = method;
            this.body = body;
            this.receivedAt = receivedAt;
        }

        String method() {
            return method;
        }

        String body() {
            return body;
        }

        Instant receivedAt() {
            return receivedAt;
        }
    }
}
