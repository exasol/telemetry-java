package com.exasol.telemetry;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.*;

final class Message {
    static final String VERSION = "0.1";

    private final Instant timestamp;
    private final Map<String, List<Instant>> features;

    private Message(final Instant timestamp, final Map<String, List<Instant>> features) {
        this.timestamp = requireNonNull(timestamp, "timestamp");
        this.features = requireNonNull(features, "features");
    }

    static Message fromEvents(final Instant timestamp, final List<TelemetryEvent> events) {
        final Map<String, List<Instant>> features = new LinkedHashMap<>();
        for (final TelemetryEvent event : events) {
            features.computeIfAbsent(event.getFeature(), ignored -> new ArrayList<>()).add(event.getTimestamp());
        }
        return new Message(timestamp, features);
    }

    String toJson() {
        final StringBuilder builder = new StringBuilder();
        builder.append('{');
        builder.append("\"version\":\"").append(VERSION).append("\",");
        builder.append("\"timestamp\":").append(timestamp.getEpochSecond()).append(',');
        builder.append("\"features\":{");

        boolean firstFeature = true;
        for (final Map.Entry<String, List<Instant>> entry : features.entrySet()) {
            if (!firstFeature) {
                builder.append(',');
            }
            builder.append('"').append(escape(entry.getKey())).append('"').append(':').append('[');
            boolean firstTimestamp = true;
            for (final Instant featureTimestamp : entry.getValue()) {
                if (!firstTimestamp) {
                    builder.append(',');
                }
                builder.append(featureTimestamp.getEpochSecond());
                firstTimestamp = false;
            }
            builder.append(']');
            firstFeature = false;
        }

        builder.append("}}");
        return builder.toString();
    }

    private static String escape(final String value) {
        final StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            final char current = value.charAt(index);
            switch (current) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(current);
                    break;
            }
        }
        return escaped.toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final Message that = (Message) other;
        return Objects.equals(timestamp, that.timestamp) && Objects.equals(features, that.features);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, features);
    }
}
