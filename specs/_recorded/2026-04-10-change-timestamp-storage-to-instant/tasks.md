# Tasks: change-timestamp-storage-to-instant

## Phase 2: Implementation
- [x] 2.1 Change `TelemetryEvent` timestamp storage from `long` to `Instant`.
- [x] 2.2 Change `TelemetryMessage` timestamp storage from `long`/`List<Long>` to `Instant`/`List<Instant>`.
- [x] 2.3 Keep JSON serialization at numeric epoch seconds.
- [x] 2.4 Update unit tests that construct `TelemetryEvent` directly.
- [x] 2.5 Confirm no permanent spec change is required.

## Phase 3: Verification
- [x] 3.1 Run targeted unit tests for event/message/transport
- [x] 3.2 Run `mvn test`
- [x] 3.3 Run `mvn verify`
- [x] 3.4 Generate verification report
