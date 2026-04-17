# Tasks: change-telemetry-failure-logging

## Phase 2: Implementation
- [x] 2.1 Add the permanent `lifecycle/status-logging` feature spec with enabled, disabled, send-count, send-failure, and stopped scenarios
- [x] 2.2 Add debug-level failure logging to the telemetry sender flow
- [x] 2.3 Extend `StatusLoggingIT` with a failed-send scenario that asserts level and message content
- [x] 2.4 Add OpenFastTrace feature, requirement, implementation, and integration-test tags for status logging

## Phase 3: Verification
- [x] 3.1 Run `mvn -Dtest=StatusLoggingIT test`
- [x] 3.2 Run `speq feature validate`
- [x] 3.3 Run `mvn generate-sources org.itsallcode:openfasttrace-maven-plugin:trace`
- [x] 3.4 Generate verification report
