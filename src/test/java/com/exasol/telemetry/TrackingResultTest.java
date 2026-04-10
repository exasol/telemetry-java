package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class TrackingResultTest {
    @Test
    void exposesExpectedEnumValues() {
        assertEquals(EnumSet.of(
                TrackingResult.ACCEPTED,
                TrackingResult.REJECTED,
                TrackingResult.DISABLED,
                TrackingResult.CLOSED),
                EnumSet.allOf(TrackingResult.class));
    }
}
