# Plan: change-telemetry-failure-logging

## Summary

This plan adds a user-visible log message when telemetry delivery fails and, because `status-logging` is only recorded historically and not yet part of the permanent spec library, it also adds the full `lifecycle/status-logging` feature to the permanent specs. The new failure log is treated as part of the same operational lifecycle logging capability.

## Design

### Context

The runtime already logs telemetry enabled, disabled, send-count, and stopped events, but it does not log failed send attempts. The permanent spec library also still lacks the `status-logging` feature entirely, so this plan must define the full feature in its intended permanent form rather than layering a delta onto a missing base feature.

- **Goals** — Add a clear operational message when telemetry sending fails, preserve the existing lifecycle logging model, and make `status-logging` part of the permanent feature library.
- **Non-Goals** — Add stack traces, payload logging, feature-name logging, retry-attempt spam, or any diagnostic logging beyond the minimal operational lifecycle set.

### Decision

Define `lifecycle/status-logging` as a permanent feature with five scenarios: enabled, disabled, send-count, send-failure, and stopped. Keep the initial status message at `INFO` and keep runtime send-related messages, including failure logs, at debug level so the runtime remains quiet unless debug logging is enabled.

### Consequences

| Decision | Alternatives Considered | Rationale |
|----------|------------------------|-----------|
| Add failure logging to `status-logging` | Create a separate failure-logging feature | Failure-on-send is part of the same telemetry lifecycle and should live with the other lifecycle messages |
| Log failures at debug level | Promote failure logs to `INFO` or `WARNING` | Preserves the existing “only initial message at INFO” contract unless a stronger escalation policy is later requested |
| Add full feature as `NEW` | Create a `CHANGED` delta against a non-existent permanent spec | The permanent spec library does not currently contain `status-logging` |

## Features

| Feature | Status | Spec |
|---------|--------|------|
| status-logging | NEW | `lifecycle/status-logging/spec.md` |

## Requirements

| Requirement | Details |
|-------------|---------|
| Permanent feature addition | `lifecycle/status-logging` SHALL be added to the permanent spec library |
| Failure log level | Failed send attempts SHALL be logged at debug level |
| Failure message content | The failure log SHALL state that telemetry sending failed and include the count of events in the failed message |
| Privacy constraint | The failure log SHALL NOT include payload bodies, tracked feature names, stack traces, or PII |

## Implementation Tasks

1. Add the permanent `lifecycle/status-logging` feature spec with the four already-implemented scenarios and the new failed-send scenario.
2. Add a debug-level log message on send failure in the telemetry sender flow.
3. Extend `StatusLoggingIT` with a scenario that triggers a failed send and asserts the failure log level and message content.
4. Update docs only if they need to mention failed-send lifecycle logging explicitly.

## Dead Code Removal

| Type | Location | Reason |
|------|----------|--------|
| None | N/A | This plan adds a lifecycle logging requirement and permanent spec coverage only |

## Verification

### Scenario Coverage

| Scenario | Test Type | Test Location | Test Name |
|----------|-----------|---------------|-----------|
| Logs when telemetry is enabled | Integration | `src/test/java/com/exasol/telemetry/StatusLoggingIT.java` | `logsWhenTelemetryIsEnabled` |
| Logs when telemetry is disabled | Integration | `src/test/java/com/exasol/telemetry/StatusLoggingIT.java` | `logsWhenTelemetryIsDisabledWithMechanism` |
| Logs message counts when telemetry is sent | Integration | `src/test/java/com/exasol/telemetry/StatusLoggingIT.java` | `logsSentMessageCount` |
| Logs when telemetry sending fails | Integration | `src/test/java/com/exasol/telemetry/StatusLoggingIT.java` | `logsWhenTelemetrySendingFails` |
| Logs when telemetry is stopped | Integration | `src/test/java/com/exasol/telemetry/StatusLoggingIT.java` | `logsWhenTelemetryStops` |

### Manual Testing

| Feature | Command | Expected Output |
|---------|---------|-----------------|
| status-logging | `mvn verify` | Tests confirm enabled, disabled, send-count, failed-send, and stopped lifecycle logs with the expected levels |

### Checklist

| Step | Command | Expected |
|------|---------|----------|
| Build | `mvn package` | Exit 0 |
| Test | `mvn test` | 0 failures |
| Lint | `mvn verify` | Exit 0 |
| Format | `mvn verify` | Exit 0; no dedicated formatter command is defined in the mission |
