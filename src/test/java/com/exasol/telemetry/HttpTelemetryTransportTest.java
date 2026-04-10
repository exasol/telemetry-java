package com.exasol.telemetry;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpTelemetryTransportTest
{
    @Test
    void sendsJsonPayloadToConfiguredConnection() throws Exception
    {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(202);
        HttpTelemetryTransport transport = new HttpTelemetryTransport(
                TelemetryConfig.builder("project").endpoint(URI.create("https://example.com")).build(),
                endpoint -> connection);

        transport.send(TelemetryMessage.fromEvents(java.util.List.of(new TelemetryEvent("project.feature", 10))));

        assertEquals("POST", connection.requestMethod);
        assertTrue(connection.doOutput);
        assertEquals("application/json", connection.contentType);
        assertTrue(connection.payloadAsString().contains("\"features\":{\"project.feature\":[10]}"));
    }

    @Test
    void rejectsNonSuccessStatusCodes() throws Exception
    {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(500);
        HttpTelemetryTransport transport = new HttpTelemetryTransport(
                TelemetryConfig.builder("project").endpoint(URI.create("https://example.com")).build(),
                endpoint -> connection);

        IOException exception = assertThrows(IOException.class,
                () -> transport.send(TelemetryMessage.fromEvents(java.util.List.of(new TelemetryEvent("project.feature", 10)))));
        assertTrue(exception.getMessage().contains("Unexpected response status"));
    }

    private static final class FakeHttpURLConnection extends HttpURLConnection
    {
        private final ByteArrayOutputStream payload = new ByteArrayOutputStream();
        private final int responseCode;
        private String requestMethod;
        private boolean doOutput;
        private String contentType;

        private FakeHttpURLConnection(int responseCode)
        {
            super(null);
            this.responseCode = responseCode;
        }

        @Override
        public void disconnect() {}

        @Override
        public boolean usingProxy()
        {
            return false;
        }

        @Override
        public void connect() {}

        @Override
        public void setRequestMethod(String method) throws ProtocolException
        {
            this.requestMethod = method;
        }

        @Override
        public void setDoOutput(boolean dooutput)
        {
            this.doOutput = dooutput;
        }

        @Override
        public void setRequestProperty(String key, String value)
        {
            if ("Content-Type".equals(key)) {
                this.contentType = value;
            }
        }

        @Override
        public OutputStream getOutputStream()
        {
            return payload;
        }

        @Override
        public int getResponseCode()
        {
            return responseCode;
        }

        private String payloadAsString()
        {
            return payload.toString(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
