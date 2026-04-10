package com.exasol.telemetry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class TelemetryEnvironmentTest
{
    @Test
    void systemEnvironmentReturnsNullForUnknownVariable()
    {
        assertNull(TelemetryEnvironment.SystemEnvironment.INSTANCE.getenv("__EXASOL_TELEMETRY_TEST_MISSING__"));
    }
}
