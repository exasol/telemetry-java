package com.exasol.telemetry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TelemetryMessage
{
    static final String VERSION = "0.1";

    private final long timestamp;
    private final Map<String, List<Long>> features;

    private TelemetryMessage(long timestamp, Map<String, List<Long>> features)
    {
        this.timestamp = timestamp;
        this.features = features;
    }

    static TelemetryMessage fromEvents(List<TelemetryEvent> events)
    {
        Map<String, List<Long>> features = new LinkedHashMap<>();
        for (TelemetryEvent event : events) {
            features.computeIfAbsent(event.getFeature(), ignored -> new ArrayList<>()).add(event.getTimestamp());
        }
        return new TelemetryMessage(Instant.now().getEpochSecond(), features);
    }

    String toJson()
    {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        builder.append("\"version\":\"").append(VERSION).append("\",");
        builder.append("\"timestamp\":").append(timestamp).append(',');
        builder.append("\"features\":{");

        boolean firstFeature = true;
        for (Map.Entry<String, List<Long>> entry : features.entrySet()) {
            if (!firstFeature) {
                builder.append(',');
            }
            builder.append('"').append(escape(entry.getKey())).append('"').append(':').append('[');
            boolean firstTimestamp = true;
            for (Long featureTimestamp : entry.getValue()) {
                if (!firstTimestamp) {
                    builder.append(',');
                }
                builder.append(featureTimestamp);
                firstTimestamp = false;
            }
            builder.append(']');
            firstFeature = false;
        }

        builder.append("}}");
        return builder.toString();
    }

    private static String escape(String value)
    {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
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
}
