package com.exasol.telemetry;

import java.util.Map;

final class MapEnvironment implements Environment {
    private final Map<String, String> values;

    MapEnvironment(final Map<String, String> values) {
        this.values = values;
    }

    static Environment empty() {
        return new MapEnvironment(Map.of());
    }

    @Override
    public String getenv(final String name) {
        return values.get(name);
    }
}
