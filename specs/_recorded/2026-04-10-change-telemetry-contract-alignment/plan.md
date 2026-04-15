# Plan: change-telemetry-contract-alignment

## Summary

This plan captures the post-MVP contract alignment that was implemented after the original telemetry library MVP recording. It updates the permanent spec library to cover the broadened environment-variable disable semantics and the already-implemented behaviors for post-close tracking and queue-drain batching.

## Features

| Feature | Status | Spec |
|---------|--------|------|
| tracking-api | CHANGED | `tracking/tracking-api/spec.md` |
| async-delivery | CHANGED | `delivery/async-delivery/spec.md` |
| tracking-controls | CHANGED | `config/tracking-controls/spec.md` |

## Requirements

| Requirement | Details |
|-------------|---------|
| Disable semantics | Any non-empty `EXASOL_TELEMETRY_DISABLE` or `CI` value SHALL disable telemetry collection and delivery |
| Closed-client behavior | Tracking after `TelemetryClient.close()` SHALL report the closed state and SHALL NOT enqueue events |
| Payload semantics | When multiple events are drained together, the sender SHALL emit one payload with timestamps grouped by fully qualified feature name |

## Implementation Tasks

1. Update the tracking-controls contract to treat any non-empty `EXASOL_TELEMETRY_DISABLE` or `CI` value as disabling telemetry.
2. Add spec coverage for post-close tracking behavior in `tracking-api`.
3. Add spec coverage for queue-drain batching semantics in `async-delivery`.
4. Align automated tests and developer-facing documentation with the updated contract.

## Dead Code Removal

| Type | Location | Reason |
|------|----------|--------|
| None | N/A | This change tightens contract coverage and visibility only; it does not remove obsolete runtime code |

## Verification

### Scenario Coverage

| Scenario | Test Type | Test Location | Test Name |
|----------|-----------|---------------|-----------|
| Records a tagged feature usage event | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `recordsTaggedFeatureUsageEvent` |
| Rejects unsupported usage payloads | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `rejectsUnsupportedUsagePayloads` |
| Rejects tracking after the client is closed | Integration | `src/test/java/com/exasol/telemetry/TelemetryClientTest.java` | `returnsClosedAfterCloseAndCloseIsIdempotent` |
| Sends queued events asynchronously over HTTP | Integration | `src/test/java/com/exasol/telemetry/AsyncDeliveryIT.java` | `sendsQueuedEventsAsynchronouslyOverHttp` |
| Retries failed delivery with exponential backoff until timeout | Integration | `src/test/java/com/exasol/telemetry/AsyncDeliveryIT.java` | `retriesFailedDeliveryWithExponentialBackoffUntilTimeout` |
| Batches multiple drained events into a single protocol message | Unit | `src/test/java/com/exasol/telemetry/TelemetryMessageTest.java` | `groupsEventsByFeatureAndSerializesProtocolShape` |
| Disables tracking via environment variables | Integration | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `disablesTrackingViaEnvironmentVariables` |
| Disables tracking automatically in CI | Integration | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `disablesTrackingAutomaticallyWhenCiIsNonEmpty` |
| Overrides the configured endpoint via environment variable | Integration | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `overridesConfiguredEndpointViaEnvironmentVariable` |

### Manual Testing

| Feature | Command | Expected Output |
|---------|---------|-----------------|
| tracking-api | `mvn verify` | Scenario-mapped tests confirm accepted, rejected, and closed tracking behaviors |
| async-delivery | `mvn verify` | Scenario-mapped tests confirm async delivery, retry timeout, and grouped protocol payload semantics |
| tracking-controls | `mvn verify` | Scenario-mapped tests confirm disablement for non-empty env values and endpoint override |

### Checklist

| Step | Command | Expected |
|------|---------|----------|
| Build | `mvn package` | Exit 0 |
| Test | `mvn test` | 0 failures |
| Lint | `mvn verify` | Exit 0 |
| Format | `mvn verify` | Exit 0; no dedicated formatter command is defined in the mission |
