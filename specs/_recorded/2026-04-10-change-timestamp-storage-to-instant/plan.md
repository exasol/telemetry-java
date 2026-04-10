# Plan: change-timestamp-storage-to-instant

## Summary

This plan records the completed internal timestamp refactor that replaced `long` epoch-second storage with `Instant` in the event and message model while keeping the JSON protocol unchanged. The runtime behavior and permanent specs remain unchanged because payload timestamps are still emitted as numeric epoch seconds.

## Requirements

| Requirement | Details |
|-------------|---------|
| Internal timestamp model | `TelemetryEvent` and `TelemetryMessage` SHALL store timestamps as `Instant` rather than `long` |
| Wire compatibility | JSON payloads SHALL continue to serialize top-level and feature timestamps as numeric epoch seconds |
| Runtime compatibility | Tracking, retry, batching, and shutdown behavior SHALL remain unchanged |

## Implementation Tasks

1. Change `TelemetryEvent` to store and expose `Instant` timestamps.
2. Change `TelemetryMessage` to store message and feature timestamps as `Instant`.
3. Keep JSON serialization converting `Instant` values to epoch seconds at the last step.
4. Update unit tests that construct `TelemetryEvent` directly so they assert `Instant` values while preserving the same JSON assertions.
5. Verify that integration tests still pass without any protocol or behavior changes.

## Dead Code Removal

| Type | Location | Reason |
|------|----------|--------|
| None | N/A | The change is a type-safe internal refactor and does not obsolete runtime code |

## Verification

### Scenario Coverage

| Scenario | Test Type | Test Location | Test Name |
|----------|-----------|---------------|-----------|
| Records a tagged feature usage event | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `recordsTaggedFeatureUsageEvent` |
| Sends queued events asynchronously over HTTP | Integration | `src/test/java/com/exasol/telemetry/AsyncDeliveryIT.java` | `sendsQueuedEventsAsynchronouslyOverHttp` |
| Retries failed delivery with exponential backoff until timeout | Integration | `src/test/java/com/exasol/telemetry/AsyncDeliveryIT.java` | `retriesFailedDeliveryWithExponentialBackoffUntilTimeout` |
| Flushes pending events on close | Integration | `src/test/java/com/exasol/telemetry/ShutdownFlushIT.java` | `flushesPendingEventsOnClose` |
| Groups events by feature and serializes protocol shape | Unit | `src/test/java/com/exasol/telemetry/TelemetryMessageTest.java` | `groupsEventsByFeatureAndSerializesProtocolShape` |
| Sends JSON payload to configured client | Unit | `src/test/java/com/exasol/telemetry/HttpTelemetryTransportTest.java` | `sendsJsonPayloadToConfiguredClient` |
| Exposes feature and timestamp | Unit | `src/test/java/com/exasol/telemetry/TelemetryEventTest.java` | `exposesFeatureAndTimestamp` |

### Manual Testing

| Feature | Command | Expected Output |
|---------|---------|-----------------|
| timestamp-storage-to-instant | `mvn verify` | Build succeeds and tests confirm `Instant` storage does not change JSON protocol or delivery behavior |

### Checklist

| Step | Command | Expected |
|------|---------|----------|
| Build | `mvn package` | Exit 0 |
| Test | `mvn test` | 0 failures |
| Lint | `mvn verify` | Exit 0 |
| Format | `mvn verify` | Exit 0; no dedicated formatter command is defined in the mission |
