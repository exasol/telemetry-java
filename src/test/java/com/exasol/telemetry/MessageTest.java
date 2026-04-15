package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class MessageTest {
    @Test
    void verifiesEqualsAndHashCode() {
        EqualsVerifier.forClass(Message.class).verify();
    }

    @Test
    void groupsEventsByFeatureAndSerializesProtocolShape() {
        final Message message = Message.fromEvents(Instant.ofEpochSecond(30), List.of(
                new TelemetryEvent("project.a", Instant.ofEpochSecond(10)),
                new TelemetryEvent("project.a", Instant.ofEpochSecond(20)),
                new TelemetryEvent("project.b", Instant.ofEpochSecond(30))));

        final String json = message.toJson();

        assertThat(json, containsString("\"version\":\"0.1\""));
        assertThat(json, containsString("\"timestamp\":"));
        assertThat(json, containsString("\"features\":{\"project.a\":[10,20],\"project.b\":[30]}"));
    }

    @Test
    void serializesValidJson() {
        final Message message = Message.fromEvents(Instant.ofEpochSecond(30), List.of(
                new TelemetryEvent("project.a", Instant.ofEpochSecond(10))));

        final var payload = JsonTestHelper.parseJson(message.toJson());

        assertThat(payload.containsKey("version"), is(true));
        assertThat(payload.containsKey("timestamp"), is(true));
        assertThat(payload.containsKey("features"), is(true));
    }

    @Test
    void escapesFeatureNamesInJson() {
        final Message message = Message.fromEvents(Instant.ofEpochSecond(30), List.of(
                new TelemetryEvent("proj.\"x\"\n\t\\", Instant.ofEpochSecond(10))));

        final String json = message.toJson();

        assertThat(json, containsString("proj.\\\"x\\\"\\n\\t\\\\"));
    }

    @Test
    void serializesEmptyFeatureCollection() {
        final Message message = Message.fromEvents(Instant.ofEpochSecond(30), List.of());

        assertThat(message.toJson(), containsString("\"features\":{}"));
    }
}
