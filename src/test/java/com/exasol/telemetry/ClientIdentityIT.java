package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ClientIdentityIT {
    // [itest~client-identity-requires-project-tag-and-product-version~1->scn~client-identity-requires-project-tag-and-product-version-when-creating-telemetry-configuration~1]
    @Test
    void requiresProjectTagAndProductVersionWhenCreatingTelemetryConfiguration() {
        final IllegalArgumentException blankProjectTag = assertThrows(IllegalArgumentException.class,
                () -> TelemetryConfig.builder(" ", "1.2.3").build());
        final IllegalArgumentException blankProductVersion = assertThrows(IllegalArgumentException.class,
                () -> TelemetryConfig.builder("project", " ").build());

        assertThat(blankProjectTag.getMessage(), containsString("projectTag"));
        assertThat(blankProductVersion.getMessage(), containsString("productVersion"));
    }
}
