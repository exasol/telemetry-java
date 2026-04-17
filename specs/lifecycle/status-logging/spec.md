# Feature: status-logging

Informs application users through operational log messages about the telemetry lifecycle, including failed delivery attempts.

## Requirement: Status Logging
`req~status-logging~1`

The library shall emit operational telemetry lifecycle log messages for enabled, disabled, send, send-failure, and stopped states as described by the scenarios below.

Covers:
* `feat~status-logging~1`

Needs: impl, itest

## Background

These log messages are informational lifecycle messages only. They do not include payload bodies, tracked feature names, stack traces, or personally identifiable information.

## Scenarios

### Scenario: Logs when telemetry is enabled

* *GIVEN* the telemetry client is created and telemetry is enabled
* *WHEN* initialization completes
* *THEN* the library SHALL log at `INFO` level that telemetry is enabled
* *AND* the library SHALL include instructions that `EXASOL_TELEMETRY_DISABLE` can disable telemetry
* *AND* the library SHALL include the actual current values of `EXASOL_TELEMETRY_DISABLE` and `CI`

### Scenario: Logs when telemetry is disabled

* *GIVEN* the telemetry client is created and telemetry is disabled
* *WHEN* initialization completes
* *THEN* the library SHALL log at `INFO` level that telemetry is disabled
* *AND* the library SHALL identify whether telemetry was disabled by `EXASOL_TELEMETRY_DISABLE` or `CI`
* *AND* the library SHALL include the actual env-var value that caused disablement

### Scenario: Logs message counts when telemetry is sent

* *GIVEN* one or more accepted telemetry events are about to be sent to the server
* *WHEN* the sender submits a telemetry message
* *THEN* the library SHALL log at debug level that telemetry is being sent to the server
* *AND* the library SHALL include the count of events in the message

### Scenario: Logs when telemetry sending fails

* *GIVEN* one or more accepted telemetry events are about to be sent to the server
* *AND* the send attempt fails
* *WHEN* the sender handles the failed send attempt
* *THEN* the library SHALL log at debug level that telemetry sending failed
* *AND* the library SHALL include the count of events in the failed message
* *AND* the library SHALL include the server status when the failed send attempt received an HTTP response
* *AND* the library SHALL include the root cause of the failed send attempt

### Scenario: Logs when telemetry is stopped

* *GIVEN* the telemetry client is closed
* *WHEN* shutdown processing completes
* *THEN* the library SHALL log at debug level that telemetry is stopped
