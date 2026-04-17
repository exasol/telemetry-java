package com.exasol.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.exasol.telemetry.TelemetryConfig.Builder;

class ClientIdentityIT {
    // [itest~client-identity-requires-project-tag-and-product-version~1->scn~client-identity-requires-project-tag-and-product-version-when-creating-telemetry-configuration~1]
    @Test
    void requiresProjectTagWhenCreatingTelemetryConfiguration() {
        final Builder builder = TelemetryConfig.builder(" ", "1.2.3");
        final IllegalArgumentException blankProjectTag = assertThrows(IllegalArgumentException.class, builder::build);

        assertThat(blankProjectTag.getMessage(), containsString("projectTag"));
    }

    // [itest~client-identity-requires-project-tag-and-product-version~1->scn~client-identity-requires-project-tag-and-product-version-when-creating-telemetry-configuration~1]
    @Test
    void requiresProductVersionWhenCreatingTelemetryConfiguration() {
        final Builder builder = TelemetryConfig.builder("project", " ");
        final IllegalArgumentException blankProductVersion = assertThrows(IllegalArgumentException.class, builder::build);

        assertThat(blankProductVersion.getMessage(), containsString("productVersion"));
    }
}
