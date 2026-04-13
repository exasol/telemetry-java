package com.exasol.telemetry;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EnvironmentTest {
    @Test
    void systemEnvironmentReturnsNullForUnknownVariable() {
        assertNull(Environment.SystemEnvironment.INSTANCE.getenv("__EXASOL_TELEMETRY_TEST_MISSING__"));
    }
}
