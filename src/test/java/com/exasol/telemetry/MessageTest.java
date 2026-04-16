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

    // [utest~message-groups-events~1->req~async-delivery~1]
    // [utest~message-emits-client-identity~1->req~client-identity~1]
    @Test
    void groupsEventsByFeatureAndSerializesProtocolShape() {
        final Message message = Message.fromEvents("shop-ui", "1.2.3", Instant.ofEpochSecond(30), List.of(
                new TelemetryEvent("project.a", Instant.ofEpochSecond(10)),
                new TelemetryEvent("project.a", Instant.ofEpochSecond(20)),
                new TelemetryEvent("project.b", Instant.ofEpochSecond(30))));

        final String json = message.toJson();

        assertThat(json, containsString("\"category\":\"shop-ui\""));
        assertThat(json, containsString("\"version\":\"0.2.0\""));
        assertThat(json, containsString("\"productVersion\":\"1.2.3\""));
        assertThat(json, containsString("\"timestamp\":"));
        assertThat(json, containsString("\"features\":{\"project.a\":[10,20],\"project.b\":[30]}"));
    }

    // [utest~message-valid-json~1->req~async-delivery~1]
    @Test
    void serializesValidJson() {
        final Message message = Message.fromEvents("shop-ui", "1.2.3", Instant.ofEpochSecond(30), List.of(
                new TelemetryEvent("project.a", Instant.ofEpochSecond(10))));

        final var payload = JsonTestHelper.parseJson(message.toJson());

        assertThat(payload.containsKey("category"), is(true));
        assertThat(payload.containsKey("version"), is(true));
        assertThat(payload.containsKey("productVersion"), is(true));
        assertThat(payload.containsKey("timestamp"), is(true));
        assertThat(payload.containsKey("features"), is(true));
    }

    // [utest~message-escapes-feature-names~1->req~async-delivery~1]
    @Test
    void escapesFeatureNamesInJson() {
        final Message message = Message.fromEvents("shop-ui", "1.2.3", Instant.ofEpochSecond(30), List.of(
                new TelemetryEvent("proj.\"x\"\n\t\\", Instant.ofEpochSecond(10))));

        final String json = message.toJson();

        assertThat(json, containsString("proj.\\\"x\\\"\\n\\t\\\\"));
    }

    // [utest~message-escapes-client-identity~1->req~client-identity~1]
    @Test
    void escapesCategoryAndProductVersionInJson() {
        final Message message = Message.fromEvents("shop-\"ui\"\n\t\\", "1.2.3-\"beta\"\n\t\\", Instant.ofEpochSecond(30), List.of(
                new TelemetryEvent("feature", Instant.ofEpochSecond(10))));

        final String json = message.toJson();

        assertThat(json, containsString("\"category\":\"shop-\\\"ui\\\"\\n\\t\\\\\""));
        assertThat(json, containsString("\"productVersion\":\"1.2.3-\\\"beta\\\"\\n\\t\\\\\""));
    }

    @Test
    void serializesEmptyFeatureCollection() {
        final Message message = Message.fromEvents("shop-ui", "1.2.3", Instant.ofEpochSecond(30), List.of());

        assertThat(message.toJson(), containsString("\"features\":{}"));
    }
}
