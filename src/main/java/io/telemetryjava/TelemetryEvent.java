package io.telemetryjava;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class TelemetryEvent
{
    private final String projectTag;
    private final String feature;
    private final Map<String, String> attributes;

    TelemetryEvent(String projectTag, String feature, Map<String, String> attributes)
    {
        this.projectTag = projectTag;
        this.feature = feature;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    String toJson()
    {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        appendField(builder, "projectTag", projectTag);
        builder.append(',');
        appendField(builder, "feature", feature);

        if (!attributes.isEmpty()) {
            builder.append(',');
            builder.append("\"attributes\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                appendField(builder, entry.getKey(), entry.getValue());
                first = false;
            }
            builder.append('}');
        }

        builder.append('}');
        return builder.toString();
    }

    private static void appendField(StringBuilder builder, String key, String value)
    {
        builder.append('"').append(escape(key)).append('"').append(':').append('"').append(escape(value)).append('"');
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
