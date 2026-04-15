# Plan: change-public-config-surface

## Summary

This plan reduces the public `TelemetryConfig.Builder` surface by hiding configuration knobs that are only useful for tests and internal tuning. It keeps the existing supported runtime contract intact by preserving the test seams that are still required for endpoint routing and retry timing.

## Requirements

| Requirement | Details |
|-------------|---------|
| Public config surface | Library users SHALL NOT configure unsupported queue and HTTP timeout tuning knobs through the public builder API |
| Supported runtime API | Public endpoint configuration and retry-timing controls SHALL remain available while tests still depend on them |
| Internal testability | Package-local tests MAY continue to set queue capacity and transport timeout knobs through package-private builder methods when needed |

## Implementation Tasks

1. Change `TelemetryConfig.Builder.queueCapacity(...)`, `TelemetryConfig.Builder.connectTimeout(...)`, and `TelemetryConfig.Builder.requestTimeout(...)` to package-private visibility.
2. Keep `endpoint(...)`, `retryTimeout(...)`, `initialRetryDelay(...)`, and `maxRetryDelay(...)` unchanged because the current integration tests depend on them.
3. Update or remove validation-only tests as needed so hidden builder methods are still covered appropriately from the package-local test scope.
4. Confirm that no permanent spec changes are required because the public runtime behavior remains unchanged.

## Dead Code Removal

| Type | Location | Reason |
|------|----------|--------|
| None | N/A | This plan narrows internal API visibility only; no runtime components become obsolete |

## Verification

### Scenario Coverage

| Scenario | Test Type | Test Location | Test Name |
|----------|-----------|---------------|-----------|
| Rejects non-positive numbers and durations | Unit | `src/test/java/com/exasol/telemetry/TelemetryConfigTest.java` | `rejectsNonPositiveNumbersAndDurations` |
| Uses defaults and configured values | Unit | `src/test/java/com/exasol/telemetry/TelemetryConfigTest.java` | `usesDefaultsAndConfiguredValues` |
| Sends queued events asynchronously over HTTP | Integration | `src/test/java/com/exasol/telemetry/AsyncDeliveryIT.java` | `sendsQueuedEventsAsynchronouslyOverHttp` |
| Retries failed delivery with exponential backoff until timeout | Integration | `src/test/java/com/exasol/telemetry/AsyncDeliveryIT.java` | `retriesFailedDeliveryWithExponentialBackoffUntilTimeout` |

### Manual Testing

| Feature | Command | Expected Output |
|---------|---------|-----------------|
| public-config-surface | `mvn verify` | Build succeeds and tests confirm hidden builder methods do not change runtime delivery behavior |

### Checklist

| Step | Command | Expected |
|------|---------|----------|
| Build | `mvn package` | Exit 0 |
| Test | `mvn test` | 0 failures |
| Lint | `mvn verify` | Exit 0 |
| Format | `mvn verify` | Exit 0; no dedicated formatter command is defined in the mission |
