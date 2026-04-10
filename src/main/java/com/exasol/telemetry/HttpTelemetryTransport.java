package com.exasol.telemetry;

import java.io.IOException;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

final class HttpTelemetryTransport {
    interface RequestSender {
        int send(HttpRequest request)
                throws IOException, InterruptedException;
    }

    private final TelemetryConfig config;
    private final RequestSender requestSender;

    HttpTelemetryTransport(final TelemetryConfig config) {
        this(config, defaultSender(config));
    }

    HttpTelemetryTransport(final TelemetryConfig config, final RequestSender requestSender) {
        this.config = config;
        this.requestSender = requestSender;
    }

    private static RequestSender defaultSender(final TelemetryConfig config) {
        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(config.getConnectTimeout())
                .build();
        return request -> client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    void send(final TelemetryMessage message)
            throws IOException {
        final HttpRequest request = HttpRequest.newBuilder(config.getEndpoint())
                .header("Content-Type", "application/json")
                .timeout(config.getRequestTimeout())
                .POST(HttpRequest.BodyPublishers.ofString(message.toJson(), StandardCharsets.UTF_8))
                .build();

        final int statusCode;
        try {
            statusCode = requestSender.send(request);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while sending telemetry", exception);
        }

        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Unexpected response status: " + statusCode);
        }
    }
}
