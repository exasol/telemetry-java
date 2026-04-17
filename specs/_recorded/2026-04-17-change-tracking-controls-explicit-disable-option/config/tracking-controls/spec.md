# Feature: tracking-controls

Adds an explicit host-configured disable option while preserving existing environment-based tracking controls.

## Background

Host applications may expose their own telemetry opt-out setting and need a direct way to propagate that choice into `telemetry-java`. Environment variables still disable telemetry automatically in deployment and CI environments, and `EXASOL_TELEMETRY_ENDPOINT` continues to override the configured endpoint independently of disablement.

## Scenarios

<!-- DELTA:NEW -->
### Scenario: Disables tracking via explicit host configuration

* *GIVEN* the host application configures telemetry as disabled in code
* *WHEN* the host application initializes the library and records feature usage
* *THEN* the library SHALL disable telemetry collection and delivery
* *AND* the library MUST NOT enqueue or send usage events while disabled
<!-- /DELTA:NEW -->
