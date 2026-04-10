package com.exasol.telemetry;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryMessageTest
{
    @Test
    void verifiesEqualsAndHashCode()
    {
        EqualsVerifier.forClass(TelemetryMessage.class).verify();
    }

    @Test
    void groupsEventsByFeatureAndSerializesProtocolShape()
    {
        TelemetryMessage message = TelemetryMessage.fromEvents(List.of(
                new TelemetryEvent("project.a", 10),
                new TelemetryEvent("project.a", 20),
                new TelemetryEvent("project.b", 30)));

        String json = message.toJson();

        assertTrue(json.contains("\"version\":\"0.1\""));
        assertTrue(json.contains("\"timestamp\":"));
        assertTrue(json.contains("\"features\":{\"project.a\":[10,20],\"project.b\":[30]}"));
    }

    @Test
    void escapesFeatureNamesInJson()
    {
        TelemetryMessage message = TelemetryMessage.fromEvents(List.of(
                new TelemetryEvent("proj.\"x\"\n\t\\", 10)));

        String json = message.toJson();

        assertTrue(json.contains("proj.\\\"x\\\"\\n\\t\\\\"));
    }

    @Test
    void serializesEmptyFeatureCollection()
    {
        TelemetryMessage message = TelemetryMessage.fromEvents(List.of());

        assertTrue(message.toJson().contains("\"features\":{}"));
    }
}
