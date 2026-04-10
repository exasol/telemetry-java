package com.exasol.telemetry;

final class TelemetryEvent
{
    private final String feature;
    private final long timestamp;

    TelemetryEvent(String feature, long timestamp)
    {
        this.feature = feature;
        this.timestamp = timestamp;
    }

    String getFeature()
    {
        return feature;
    }

    long getTimestamp()
    {
        return timestamp;
    }
}
