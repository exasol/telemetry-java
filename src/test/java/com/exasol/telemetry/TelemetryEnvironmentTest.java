package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TelemetryEnvironmentTest {
    @Test
    void systemEnvironmentReturnsNullForUnknownVariable() {
        assertNull(TelemetryEnvironment.SystemEnvironment.INSTANCE.getenv("__EXASOL_TELEMETRY_TEST_MISSING__"));
    }
}
