package com.exasol.telemetry;

import java.io.IOException;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

final class HttpTelemetryTransport {
    static final class Response {
        private final int statusCode;
        private final String body;

        Response(final int statusCode, final String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        int getStatusCode() {
            return statusCode;
        }

        String getBody() {
            return body;
        }
    }

    interface RequestSender {
        Response send(HttpRequest request)
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
        return request -> {
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(response.statusCode(), response.body());
        };
    }

    void send(final TelemetryMessage message)
            throws IOException {
        final HttpRequest request = HttpRequest.newBuilder(config.getEndpoint())
                .header("Content-Type", "application/json")
                .timeout(config.getRequestTimeout())
                .POST(HttpRequest.BodyPublishers.ofString(message.toJson(), StandardCharsets.UTF_8))
                .build();

        final Response response;
        try {
            response = requestSender.send(request);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while sending telemetry", exception);
        }

        final int statusCode = response.getStatusCode();
        if (statusCode < 200 || statusCode >= 300) {
            final String serverResponse = response.getBody();
            throw new TelemetryHttpException(statusCode,
                    (serverResponse == null || serverResponse.isBlank()) ? "Unexpected response status: " + statusCode : serverResponse);
        }
    }
}
