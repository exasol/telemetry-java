package com.exasol.telemetry;

import java.util.Map;

final class MapTelemetryEnvironment implements TelemetryEnvironment
{
    private final Map<String, String> values;

    MapTelemetryEnvironment(Map<String, String> values)
    {
        this.values = values;
    }

    @Override
    public String getenv(String name)
    {
        return values.get(name);
    }
}
