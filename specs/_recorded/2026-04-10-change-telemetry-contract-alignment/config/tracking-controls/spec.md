# Feature: tracking-controls

Aligns tracking controls with the implemented environment-variable disable semantics.

## Background

The runtime already treats any non-empty `EXASOL_TELEMETRY_DISABLE` or `CI` value as disabling telemetry. This delta updates the feature scenarios to match that behavior.

## Scenarios

<!-- DELTA:CHANGED -->
### Scenario: Disables tracking via environment variables

* *GIVEN* the host environment sets `EXASOL_TELEMETRY_DISABLE` to a non-empty value
* *WHEN* the host application initializes the library and records feature usage
* *THEN* the library SHALL disable telemetry collection and delivery
* *AND* the library MUST NOT enqueue or send usage events while disabled
<!-- /DELTA:CHANGED -->

<!-- DELTA:CHANGED -->
### Scenario: Disables tracking automatically in CI

* *GIVEN* the host environment sets `CI` to a non-empty value
* *WHEN* the host application initializes the library and records feature usage
* *THEN* the library SHALL disable telemetry collection and delivery
* *AND* the library MUST NOT enqueue or send usage events while disabled
<!-- /DELTA:CHANGED -->
