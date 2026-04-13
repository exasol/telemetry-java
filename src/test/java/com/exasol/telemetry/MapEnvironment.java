package com.exasol.telemetry;

import java.util.Map;

final class MapEnvironment implements Environment {
    private final Map<String, String> values;

    MapEnvironment(final Map<String, String> values) {
        this.values = values;
    }

    @Override
    public String getenv(final String name) {
        return values.get(name);
    }
}
