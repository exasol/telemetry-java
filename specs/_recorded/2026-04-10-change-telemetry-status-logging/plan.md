# Plan: change-telemetry-status-logging

## Summary

This plan adds explicit operational status log messages so application users can see when telemetry is enabled, disabled, actively sending data, and stopped. It also changes the mission/spec baseline to allow these informational lifecycle logs as a narrow exception to the current “logs are out of scope” statement.

## Design

### Context

The current library is intentionally minimal and does not emit operational messages, but the product requirement is now to inform users through logs when telemetry starts, why it is disabled, when payloads are sent, and when telemetry stops. The existing mission treats logs as out of scope, so the plan must explicitly narrow that restriction rather than silently violating it.

- **Goals** — Expose a small, predictable set of user-visible telemetry lifecycle messages without changing delivery behavior or payload contents.
- **Non-Goals** — Add verbose diagnostic logging, payload/body logging, retry-attempt logging, stack traces, or any logging of tracked feature names or PII.

### Decision

Use JDK logging already available in the runtime to emit lifecycle messages from the telemetry client and sender flow. The initial enabled/disabled status message is logged at `INFO`, while send-count and stopped messages are logged at debug level. The messages are operational only, must not include payload contents, and must state either the disable mechanism or the number of events being sent.

#### Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| Lifecycle logging | `TelemetryClient` creation and shutdown | Centralizes enabled/disabled/stopped messages around client state transitions |
| Operational event logging | Sender flow before transport send | Exposes message-count visibility without logging payload contents |
| Reason-coded messaging | Disable-path selection | Ensures the disabled message tells users whether env opt-out or CI disabled telemetry |

### Consequences

| Decision | Alternatives Considered | Rationale |
|----------|------------------------|-----------|
| Allow a narrow class of INFO logs | Keep all logging out of scope | The user requirement explicitly asks for runtime status visibility |
| Log message counts only | Log full payloads or feature names | Count-only logging informs users without leaking tracked data |
| Emit one stop message on close | Silent shutdown | Completes the lifecycle contract with a visible terminal state |

## Features

| Feature | Status | Spec |
|---------|--------|------|
| status-logging | NEW | `lifecycle/status-logging/spec.md` |

## Requirements

| Requirement | Details |
|-------------|---------|
| Mission change | The mission SHALL allow operational telemetry lifecycle logs as an exception to the previous “logs” exclusion |
| Initial message level | Client creation SHALL log the initial enabled/disabled status message at `INFO` |
| Enabled message | Client creation with telemetry enabled SHALL log that telemetry is enabled, include disable instructions, and include the actual relevant env-var values |
| Disabled message | Client creation with telemetry disabled SHALL log that telemetry is disabled, identify the disabling mechanism, and include the actual env-var value that caused disablement |
| Runtime message level | Send-count and stopped lifecycle messages SHALL be logged at debug level |
| Send message | Each successful sender submission attempt SHALL log how many events are being sent to the server |
| Stop message | Client shutdown completion SHALL log that telemetry has stopped |

## Implementation Tasks

1. Add a new `lifecycle/status-logging` feature spec covering enabled, disabled, send-count, and stopped messages.
2. Update `specs/mission.md` to narrow the “logs” out-of-scope rule so operational telemetry lifecycle logs are explicitly allowed.
3. Introduce JDK logger usage in the telemetry runtime at the client lifecycle and send points.
4. Define deterministic message text for:
   - telemetry enabled, including disable guidance via `EXASOL_TELEMETRY_DISABLE` and the actual current values of `EXASOL_TELEMETRY_DISABLE` and `CI`
   - telemetry disabled, including whether disablement came from `EXASOL_TELEMETRY_DISABLE` or `CI` and the actual env-var value that triggered disablement
   - messages being sent, including the event count
   - telemetry stopped
5. Add tests that capture emitted log records and assert both level and message content without asserting implementation-only formatting.
6. Update user-facing docs to mention that the library emits informational telemetry lifecycle logs.

## Dead Code Removal

| Type | Location | Reason |
|------|----------|--------|
| None | N/A | This plan adds an operational capability; no existing runtime code becomes obsolete |

## Verification

### Scenario Coverage

| Scenario | Test Type | Test Location | Test Name |
|----------|-----------|---------------|-----------|
| Logs when telemetry is enabled | Integration | `src/test/java/com/exasol/telemetry/StatusLoggingIT.java` | `logsWhenTelemetryIsEnabled` |
| Logs when telemetry is disabled | Integration | `src/test/java/com/exasol/telemetry/StatusLoggingIT.java` | `logsWhenTelemetryIsDisabledWithMechanism` |
| Logs message counts when telemetry is sent | Integration | `src/test/java/com/exasol/telemetry/StatusLoggingIT.java` | `logsSentMessageCount` |
| Logs when telemetry is stopped | Integration | `src/test/java/com/exasol/telemetry/StatusLoggingIT.java` | `logsWhenTelemetryStops` |

### Manual Testing

| Feature | Command | Expected Output |
|---------|---------|-----------------|
| status-logging | `mvn verify` | Tests confirm INFO log messages for enabled, disabled, sent-count, and stopped lifecycle events |

### Checklist

| Step | Command | Expected |
|------|---------|----------|
| Build | `mvn package` | Exit 0 |
| Test | `mvn test` | 0 failures |
| Lint | `mvn verify` | Exit 0 |
| Format | `mvn verify` | Exit 0; no dedicated formatter command is defined in the mission |
