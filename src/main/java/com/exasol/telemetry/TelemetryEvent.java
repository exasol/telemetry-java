package com.exasol.telemetry;

import java.time.Instant;
import java.util.Objects;

final class TelemetryEvent {
    private final String feature;
    private final Instant timestamp;

    TelemetryEvent(final String feature, final Instant timestamp) {
        this.feature = feature;
        this.timestamp = timestamp;
    }

    String getFeature() {
        return feature;
    }

    Instant getTimestamp() {
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
        return Objects.equals(feature, that.feature) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feature, timestamp);
    }
}
