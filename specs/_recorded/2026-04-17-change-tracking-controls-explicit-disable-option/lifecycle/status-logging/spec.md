# Feature: status-logging

Updates disabled lifecycle logging so it reports host-configured telemetry disablement distinctly from environment-based disablement.

## Background

The telemetry lifecycle log already reports when telemetry is enabled, disabled, sending, or stopped. Once host applications can disable telemetry explicitly in code, the disabled message needs to tell users whether disablement came from host configuration or from `EXASOL_TELEMETRY_DISABLE` or `CI`.

## Scenarios

<!-- DELTA:CHANGED -->
### Scenario: Logs when telemetry is disabled

* *GIVEN* the telemetry client is created and telemetry is disabled
* *WHEN* initialization completes
* *THEN* the library SHALL log at `INFO` level that telemetry is disabled
* *AND* the library SHALL identify whether telemetry was disabled by host configuration, `EXASOL_TELEMETRY_DISABLE`, or `CI`
* *AND* the library SHALL include the actual env-var value that caused disablement when disablement came from `EXASOL_TELEMETRY_DISABLE` or `CI`
<!-- /DELTA:CHANGED -->
