# Feature: tracking-controls

Allows host applications and deployment environments to disable tracking or redirect telemetry delivery without code changes.

## Requirement: Tracking Controls
`req~tracking-controls~1`

The library shall resolve tracking controls from configuration and environment variables so telemetry can be disabled automatically or redirected to an overridden endpoint as described by the scenarios below.

Covers:
* `feat~tracking-controls~1`

Needs: impl, utest, itest

## Background

Tracking can be deactivated by `EXASOL_TELEMETRY_DISABLE` or automatically by `CI` when either environment variable is set to any non-empty value. If the host application does not configure an endpoint, the library uses `https://metrics.exasol.com`. The configured endpoint can be overridden by `EXASOL_TELEMETRY_ENDPOINT`.

## Scenarios

### Scenario: Disables tracking via environment variables

* *GIVEN* the host environment sets `EXASOL_TELEMETRY_DISABLE` to a non-empty value
* *WHEN* the host application initializes the library and records feature usage
* *THEN* the library SHALL disable telemetry collection and delivery
* *AND* the library MUST NOT enqueue or send usage events while disabled

### Scenario: Disables tracking automatically in CI

* *GIVEN* the host environment sets `CI` to a non-empty value
* *WHEN* the host application initializes the library and records feature usage
* *THEN* the library SHALL disable telemetry collection and delivery
* *AND* the library MUST NOT enqueue or send usage events while disabled

### Scenario: Overrides the configured endpoint via environment variable

* *GIVEN* the host application configures an endpoint in code
* *AND* the host environment defines an endpoint override
* *WHEN* the library initializes
* *THEN* the library SHALL use the environment-provided endpoint for delivery
* *AND* the library SHALL continue to add the configured project short tag to all accepted events
