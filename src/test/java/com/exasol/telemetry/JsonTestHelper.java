package com.exasol.telemetry;

import jakarta.json.*;

import java.io.StringReader;

final class JsonTestHelper {
    private JsonTestHelper() {
    }

    static JsonObject parseJson(final String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
