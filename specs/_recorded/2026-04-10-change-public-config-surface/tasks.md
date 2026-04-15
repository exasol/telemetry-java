# Tasks: change-public-config-surface

## Phase 2: Implementation
- [x] 2.1 Confirm `TelemetryConfig.Builder.queueCapacity(...)` is package-private.
- [x] 2.2 Confirm `TelemetryConfig.Builder.connectTimeout(...)` is package-private.
- [x] 2.3 Confirm `TelemetryConfig.Builder.requestTimeout(...)` is package-private.
- [x] 2.4 Confirm `endpoint(...)`, `retryTimeout(...)`, `initialRetryDelay(...)`, and `maxRetryDelay(...)` remain unchanged.
- [x] 2.5 Confirm no permanent spec changes are required because public runtime behavior is unchanged.

## Phase 3: Verification
- [x] 3.1 Run `mvn test`
- [x] 3.2 Run `mvn verify`
- [x] 3.3 Generate verification report
