package com.exasol.telemetry;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

final class HttpTelemetryTransport
{
    interface RequestSender
    {
        int send(HttpRequest request)
                throws IOException, InterruptedException;
    }

    private final TelemetryConfig config;
    private final RequestSender requestSender;

    HttpTelemetryTransport(TelemetryConfig config)
    {
        this(config, defaultSender(config));
    }

    HttpTelemetryTransport(TelemetryConfig config, RequestSender requestSender)
    {
        this.config = config;
        this.requestSender = requestSender;
    }

    private static RequestSender defaultSender(TelemetryConfig config)
    {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(config.getConnectTimeout())
                .build();
        return request -> client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    void send(TelemetryMessage message)
            throws IOException
    {
        HttpRequest request = HttpRequest.newBuilder(config.getEndpoint())
                .header("Content-Type", "application/json")
                .timeout(config.getRequestTimeout())
                .POST(HttpRequest.BodyPublishers.ofString(message.toJson(), StandardCharsets.UTF_8))
                .build();

        final int statusCode;
        try {
            statusCode = requestSender.send(request);
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while sending telemetry", exception);
        }

        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Unexpected response status: " + statusCode);
        }
    }
}
