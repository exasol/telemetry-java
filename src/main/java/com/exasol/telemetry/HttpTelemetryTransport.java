package com.exasol.telemetry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

final class HttpTelemetryTransport
{
    interface ConnectionFactory
    {
        HttpURLConnection openConnection(URI endpoint) throws IOException;
    }

    private final TelemetryConfig config;
    private final ConnectionFactory connectionFactory;

    HttpTelemetryTransport(TelemetryConfig config)
    {
        this(config, endpoint -> (HttpURLConnection) endpoint.toURL().openConnection());
    }

    HttpTelemetryTransport(TelemetryConfig config, ConnectionFactory connectionFactory)
    {
        this.config = config;
        this.connectionFactory = connectionFactory;
    }

    void send(TelemetryMessage message)
            throws IOException
    {
        HttpURLConnection connection = connectionFactory.openConnection(config.getEndpoint());
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout((int) config.getConnectTimeout().toMillis());
        connection.setReadTimeout((int) config.getRequestTimeout().toMillis());
        connection.setRequestProperty("Content-Type", "application/json");

        byte[] payload = message.toJson().getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(payload);
        }

        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Unexpected response status: " + statusCode);
        }
    }
}
