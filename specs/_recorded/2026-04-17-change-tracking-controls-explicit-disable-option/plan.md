# Plan: change-tracking-controls-explicit-disable-option

## Summary

This plan extends `tracking-controls` with an explicit host-configured disable option so applications and libraries can turn telemetry off from their own configuration model without relying on process-level environment variables. It also updates status logging so the disabled message tells users whether telemetry was disabled by host configuration or by environment-based controls.

## Design

### Context

The library currently disables telemetry only when `EXASOL_TELEMETRY_DISABLE` or `CI` is set in the environment, and the disabled log message only reports those environment-driven mechanisms. Issue `#9` requires a host-facing configuration option because some integrating applications expose their own telemetry flag, for example `TELEMETRY=false`, and need a direct way to map that setting into `telemetry-java` while still producing an accurate lifecycle log message.

- **Goals** — Add an explicit disable control to the public configuration surface, keep integration simple for host applications, preserve existing environment-driven disable behavior, and make the disabled lifecycle log identify the correct disable mechanism.
- **Non-Goals** — Add a general configuration framework, allow explicit configuration to re-enable telemetry when environment-based disablement is active, or change payload shape, retry behavior, or endpoint override semantics beyond the disabled log message.

### Decision

Add a builder-level disable switch to `TelemetryConfig` and define disabled tracking as the logical OR of explicit configuration, `EXASOL_TELEMETRY_DISABLE`, and `CI`. The explicit option gives host applications a stable integration point for their own settings, while environment variables remain authoritative opt-out mechanisms for deployment environments and CI, and the status log reports whichever mechanism actually disabled telemetry.

#### Architecture

`host app config -> TelemetryConfig.Builder.disableTracking(...) -> TelemetryConfig -> TelemetryClient.create(...) -> disabled no-op client or async client`

#### Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| Builder option | `TelemetryConfig.Builder` | Matches the existing public configuration surface and keeps host integration explicit |
| Monotonic disable resolution | `TelemetryConfig` | Multiple disable mechanisms compose safely without creating override ambiguity |
| No-op runtime branch | `TelemetryClient.create(...)` | Reuses the existing disabled-client path instead of adding new runtime states |
| Mechanism-aware lifecycle log | `TelemetryClient.logDisabled(...)` | Keeps the disabled status message aligned with the configured disable source |

### Consequences

| Decision | Alternatives Considered | Rationale |
|----------|------------------------|-----------|
| Add explicit disablement on `TelemetryConfig.Builder` | Add a separate factory or system-property-based switch | The builder is already the public configuration entry point and keeps the API small |
| Treat disablement as explicit-config OR environment OR CI | Let explicit config override environment disablement | Environment and CI opt-out remain the safer, deployment-controlled mechanisms |
| Update the disabled log to report host configuration when applicable | Leave the log environment-specific or make it generic | Users should see why telemetry is disabled without losing the distinction between host-configured and environment-configured disablement |
| Keep endpoint override and identity behavior unchanged | Revisit all tracking control rules together | Issue `#9` is scoped to host-controlled disablement only |

## Features

| Feature | Status | Spec |
|---------|--------|------|
| tracking-controls | CHANGED | `config/tracking-controls/spec.md` |
| status-logging | NEW | `lifecycle/status-logging/spec.md` |

## Requirements

| Requirement | Details |
|-------------|---------|
| Explicit disable API | The public configuration surface SHALL provide a host-controlled way to disable telemetry without environment variables |
| Disable precedence | Explicit configuration, `EXASOL_TELEMETRY_DISABLE`, and `CI` SHALL each disable tracking independently |
| No-op behavior | Explicitly disabled tracking SHALL keep collection and delivery as no-ops |
| Disabled log accuracy | The disabled lifecycle log SHALL identify whether disablement came from host configuration, `EXASOL_TELEMETRY_DISABLE`, or `CI` |
| Compatibility | Existing endpoint override and environment-based disable scenarios SHALL remain unchanged |

## Implementation Tasks

1. Add a builder-level disable option to `TelemetryConfig` and include it in disabled-state resolution.
2. Extend the `tracking-controls` spec with a scenario for explicit host-configured disablement.
3. Extend `status-logging` so the disabled `INFO` message distinguishes host-configured disablement from `EXASOL_TELEMETRY_DISABLE` and `CI`.
4. Add unit coverage for disabled-state resolution when the explicit option is set alone and alongside environment variables.
5. Add or extend integration coverage to verify that explicitly disabled telemetry does not emit requests and logs the correct disable mechanism.
6. Update user-facing docs where configuration-based disablement is described for integrators or end users.

## Dead Code Removal

| Type | Location | Reason |
|------|----------|--------|
| None | N/A | The change extends existing tracking-control behavior without removing a feature |

## Verification

### Scenario Coverage

| Scenario | Test Type | Test Location | Test Name |
|----------|-----------|---------------|-----------|
| Disables tracking via explicit host configuration | Integration | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `disablesTrackingViaExplicitConfiguration` |
| Disables tracking via environment variables | Integration | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `disablesTrackingViaEnvironmentVariables` |
| Disables tracking automatically in CI | Integration | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `disablesTrackingAutomaticallyWhenCiIsNonEmpty` |
| Overrides the configured endpoint via environment variable | Integration | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `overridesConfiguredEndpointViaEnvironmentVariable` |
| Logs when telemetry is disabled | Integration | `src/test/java/com/exasol/telemetry/StatusLoggingIT.java` | `logsWhenTelemetryIsDisabledWithMechanism` |

### Manual Testing

| Feature | Command | Expected Output |
|---------|---------|-----------------|
| tracking-controls | `mvn verify` | Tests confirm explicit disablement and environment-driven controls keep telemetry disabled and preserve endpoint override behavior |
| status-logging | `mvn verify` | Tests confirm the disabled `INFO` log reports host configuration, `EXASOL_TELEMETRY_DISABLE`, or `CI` as the disable mechanism |

### Checklist

| Step | Command | Expected |
|------|---------|----------|
| Build | `mvn package` | Exit 0 |
| Test | `mvn test` | 0 failures |
| Lint | `mvn verify` | Exit 0 |
| Format | `mvn verify` | Exit 0; no dedicated formatter command is defined in the mission |
