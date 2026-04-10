package com.exasol.telemetry;

import java.util.Map;

final class MapTelemetryEnvironment implements TelemetryEnvironment {
    private final Map<String, String> values;

    MapTelemetryEnvironment(final Map<String, String> values) {
        this.values = values;
    }

    @Override
    public String getenv(final String name) {
        return values.get(name);
    }
}
