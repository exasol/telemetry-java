package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class TelemetryMessageTest {
    @Test
    void verifiesEqualsAndHashCode() {
        EqualsVerifier.forClass(TelemetryMessage.class).verify();
    }

    @Test
    void groupsEventsByFeatureAndSerializesProtocolShape() {
        final TelemetryMessage message = TelemetryMessage.fromEvents(List.of(
                new TelemetryEvent("project.a", Instant.ofEpochSecond(10)),
                new TelemetryEvent("project.a", Instant.ofEpochSecond(20)),
                new TelemetryEvent("project.b", Instant.ofEpochSecond(30))));

        final String json = message.toJson();

        assertTrue(json.contains("\"version\":\"0.1\""));
        assertTrue(json.contains("\"timestamp\":"));
        assertTrue(json.contains("\"features\":{\"project.a\":[10,20],\"project.b\":[30]}"));
    }

    @Test
    void escapesFeatureNamesInJson() {
        final TelemetryMessage message = TelemetryMessage.fromEvents(List.of(
                new TelemetryEvent("proj.\"x\"\n\t\\", Instant.ofEpochSecond(10))));

        final String json = message.toJson();

        assertTrue(json.contains("proj.\\\"x\\\"\\n\\t\\\\"));
    }

    @Test
    void serializesEmptyFeatureCollection() {
        final TelemetryMessage message = TelemetryMessage.fromEvents(List.of());

        assertTrue(message.toJson().contains("\"features\":{}"));
    }
}
