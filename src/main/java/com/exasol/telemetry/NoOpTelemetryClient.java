package com.exasol.telemetry;

final class NoOpTelemetryClient implements TelemetryClient {
    NoOpTelemetryClient() {
        // Intentionally empty.
    }

    @Override
    // [impl~no-op-telemetry-client-ignores-tracking-without-overhead~1->scn~tracking-api-makes-disabled-tracking-a-no-op-without-telemetry-overhead~1]
    public void track(final String feature) {
        // Intentionally does nothing.
    }

    @Override
    public void close() {
        // Intentionally does nothing.
    }
}
