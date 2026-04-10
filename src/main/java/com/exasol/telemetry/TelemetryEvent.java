package com.exasol.telemetry;

import java.util.Objects;

final class TelemetryEvent {
    private final String feature;
    private final long timestamp;

    TelemetryEvent(final String feature, final long timestamp) {
        this.feature = feature;
        this.timestamp = timestamp;
    }

    String getFeature() {
        return feature;
    }

    long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final TelemetryEvent that = (TelemetryEvent) other;
        return timestamp == that.timestamp && Objects.equals(feature, that.feature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feature, timestamp);
    }
}
