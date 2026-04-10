package com.exasol.telemetry;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrackingResultTest
{
    @Test
    void exposesExpectedEnumValues()
    {
        assertEquals(EnumSet.of(
                        TrackingResult.ACCEPTED,
                        TrackingResult.REJECTED,
                        TrackingResult.DISABLED,
                        TrackingResult.CLOSED),
                EnumSet.allOf(TrackingResult.class));
    }
}
