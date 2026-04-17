package com.exasol.telemetry;

final class NoOpTelemetryClient implements TelemetryClient {
    NoOpTelemetryClient() {
        // Intentionally empty.
    }

    @Override
    public void track(final String feature) {
        // Intentionally does nothing.
    }

    @Override
    public void close() {
        // Intentionally does nothing.
    }
}
