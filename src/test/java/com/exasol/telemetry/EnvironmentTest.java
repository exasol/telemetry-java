package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class EnvironmentTest {
    @Test
    void systemEnvironmentReturnsNullForUnknownVariable() {
        assertThat(Environment.SystemEnvironment.INSTANCE.getenv("__EXASOL_TELEMETRY_TEST_MISSING__"), nullValue());
    }
}
