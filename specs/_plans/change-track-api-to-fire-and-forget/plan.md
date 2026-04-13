# Plan: change-track-api-to-fire-and-forget

## Summary

This plan simplifies the public tracking API by removing the `attributes` overload and removing the `TrackingResult` return value from `track`. After this change, host applications call `track(feature)` as a fire-and-forget operation. Valid events are enqueued for delivery when tracking is active, and invalid, disabled, or closed-state calls become silent no-ops.

## Design

### Context

The current API exposes two contracts that are larger than the intended minimal library surface:

- a second `track(feature, attributes)` overload even though attributes are not part of the supported telemetry model
- a `TrackingResult` enum that forces callers to handle internal delivery-admission states

The mission emphasizes minimal integration effort and only strings plus feature names. A single fire-and-forget `track(feature)` method is a better fit for that boundary.

- **Goals** — Remove unsupported payload arguments from the public API, remove caller-visible admission-state handling, and keep event delivery semantics unchanged for valid feature names.
- **Non-Goals** — Change queueing, retry, shutdown, disablement rules, or protocol payload structure.

### Decision

Reduce the public tracking API to one method: `track(String feature)`. The method returns `void`. Calls with invalid feature names, disabled telemetry, or a closed client do not throw and do not enqueue events.

### Consequences

| Decision | Alternatives Considered | Rationale |
|----------|------------------------|-----------|
| Remove `track(feature, attributes)` | Keep overload and ignore attributes | Unsupported payload fields should not remain in the public API if they are never meant to be used |
| Remove `TrackingResult` return value | Keep return value for disabled/closed/invalid states | Caller-visible admission states increase integration effort without adding value to the minimal telemetry contract |
| Make disabled/closed/invalid calls silent no-ops | Throw exceptions on invalid or closed calls | Fire-and-forget usage keeps integration simple and avoids turning telemetry into control-flow logic |

## Features

| Feature | Status | Spec |
|---------|--------|------|
| tracking-api | CHANGED | `tracking/tracking-api/spec.md` |

## Requirements

| Requirement | Details |
|-------------|---------|
| Single tracking method | The public API SHALL expose only `track(String feature)` for recording feature usage |
| No return contract | `track` SHALL return no value |
| Silent ignore behavior | Invalid feature names, disabled telemetry, and closed-client calls SHALL NOT enqueue events and SHALL NOT throw |
| Attribute removal | The public API SHALL NOT accept arbitrary attributes for tracked events |

## Implementation Tasks

1. Remove the `track(feature, attributes)` overload and any now-unused attribute validation logic from the production API.
2. Change `track(feature)` to return `void` and update the implementation to treat disabled, invalid, queue-full, and closed states as silent no-ops.
3. Remove or repurpose `TrackingResult` and update unit and integration tests to assert side effects instead of return values.
4. Update user-facing docs to show the simplified tracking call shape.

## Dead Code Removal

| Type | Location | Reason |
|------|----------|--------|
| Public API | `TelemetryClient.track(String, Map<String, ?>)` | Unsupported attributes are being removed from the contract |
| Public API | `TrackingResult` | The fire-and-forget API no longer exposes admission-state return values |
| Internal validation | Attribute sanitization logic in `TelemetryClient` | Becomes unreachable once attributes are removed |

## Verification

### Scenario Coverage

| Scenario | Test Type | Test Location | Test Name |
|----------|-----------|---------------|-----------|
| Records a tagged feature usage event | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `recordsTaggedFeatureEvent` |
| Ignores invalid feature names | Unit | `src/test/java/com/exasol/telemetry/TelemetryClientTest.java` | `ignoresInvalidFeatureNames` |
| Ignores tracking after the client is closed | Unit | `src/test/java/com/exasol/telemetry/TelemetryClientTest.java` | `ignoresTrackingAfterClose` |

### Manual Testing

| Feature | Command | Expected Output |
|---------|---------|-----------------|
| tracking-api | `mvn verify` | Updated tests pass with the simplified `track(feature)` API and no return-value assertions |

### Checklist

| Step | Command | Expected |
|------|---------|----------|
| Build | `mvn package` | Exit 0 |
| Test | `mvn test` | 0 failures |
| Lint | `mvn verify` | Exit 0 |
| Format | `mvn verify` | Exit 0; no dedicated formatter command is defined in the mission |
