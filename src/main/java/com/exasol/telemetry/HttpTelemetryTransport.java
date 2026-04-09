package com.exasol.telemetry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

final class HttpTelemetryTransport
{
    private final TelemetryConfig config;

    HttpTelemetryTransport(TelemetryConfig config)
    {
        this.config = config;
    }

    void send(TelemetryEvent event)
            throws IOException
    {
        HttpURLConnection connection = (HttpURLConnection) config.getEndpoint().toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout((int) config.getConnectTimeout().toMillis());
        connection.setReadTimeout((int) config.getRequestTimeout().toMillis());
        connection.setRequestProperty("Content-Type", "application/json");

        byte[] payload = event.toJson().getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(payload);
        }

        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Unexpected response status: " + statusCode);
        }
    }
}
