# Feature: tracking-controls

Allows host applications and deployment environments to disable tracking or redirect telemetry delivery without code changes.

## Requirement: Tracking Controls
`req~tracking-controls~1`

The library shall resolve tracking controls from configuration and environment variables so telemetry can be disabled automatically or redirected to an overridden endpoint as described by the scenarios below.

Covers:
* `feat~tracking-controls~1`

Needs: scn

## Background

Tracking can be deactivated by `EXASOL_TELEMETRY_DISABLE` or automatically by `CI` when either environment variable is set to any non-empty value. If the host application does not configure an endpoint, the library uses `https://metrics.exasol.com/telemetry`. The configured endpoint can be overridden by `EXASOL_TELEMETRY_ENDPOINT`.

## Scenarios

### Scenario: Uses default telemetry endpoint
`scn~tracking-controls-uses-default-telemetry-endpoint~1`

* *GIVEN* the host application does not configure a telemetry endpoint
* *AND* the host environment does not define `EXASOL_TELEMETRY_ENDPOINT`
* *WHEN* the library initializes
* *THEN* the library SHALL use `https://metrics.exasol.com/telemetry` for delivery

Covers:
* `req~tracking-controls~1`

Needs: impl, utest

### Scenario: Disables tracking via environment variables
`scn~tracking-controls-disables-tracking-via-environment-variables~1`

* *GIVEN* the host environment sets `EXASOL_TELEMETRY_DISABLE` to a non-empty value
* *WHEN* the host application initializes the library and records feature usage
* *THEN* the library SHALL disable telemetry collection and delivery
* *AND* the library MUST NOT enqueue or send usage events while disabled

Covers:
* `req~tracking-controls~1`

Needs: impl, utest, itest

### Scenario: Disables tracking automatically in CI
`scn~tracking-controls-disables-tracking-automatically-in-ci~1`

* *GIVEN* the host environment sets `CI` to a non-empty value
* *WHEN* the host application initializes the library and records feature usage
* *THEN* the library SHALL disable telemetry collection and delivery
* *AND* the library MUST NOT enqueue or send usage events while disabled

Covers:
* `req~tracking-controls~1`

Needs: impl, utest, itest

### Scenario: Disables tracking via explicit host configuration
`scn~tracking-controls-disables-tracking-via-explicit-host-configuration~1`

* *GIVEN* the host application configures telemetry as disabled in code
* *WHEN* the host application initializes the library and records feature usage
* *THEN* the library SHALL disable telemetry collection and delivery
* *AND* the library MUST NOT enqueue or send usage events while disabled

Covers:
* `req~tracking-controls~1`

Needs: impl, utest, itest

### Scenario: Overrides the configured endpoint via environment variable
`scn~tracking-controls-overrides-the-configured-endpoint-via-environment-variable~1`

* *GIVEN* the host application configures an endpoint, project tag, and `productVersion` in code
* *AND* the host environment defines an endpoint override
* *WHEN* the library initializes
* *THEN* the library SHALL use the environment-provided endpoint for delivery
* *AND* the library SHALL continue to emit the configured project tag as the `category` field
* *AND* the library SHALL continue to emit the configured `productVersion` as the `productVersion` field
* *AND* the library SHALL continue to emit protocol `version`=`0.2.0`

Covers:
* `req~tracking-controls~1`

Needs: impl, utest, itest
