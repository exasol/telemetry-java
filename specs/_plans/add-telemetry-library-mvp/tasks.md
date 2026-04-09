# Tasks: add-telemetry-library-mvp

## Phase 2: Implementation (Group A)
- [ ] 2.1 Define the public configuration and tracking API, including required project short tag and environment-variable overrides.
- [ ] 2.2 Implement event validation and JSON payload shaping for allowed telemetry fields only.

## Phase 2: Implementation (Group B)
- [ ] 2.3 Implement a bounded in-memory queue and background sender that performs HTTP `POST` delivery.
- [ ] 2.4 Implement retry handling with exponential backoff and retry-timeout enforcement.

## Phase 2: Implementation (Group C)
- [ ] 2.5 Implement `AutoCloseable` shutdown logic that drains pending events and terminates background threads.
- [ ] 2.6 Add Maven integration tests that cover all scenarios with a controllable local HTTP test endpoint.

## Phase 2: Implementation (Group D)
- [ ] 2.7 Document startup configuration and shutdown expectations in the developer and app user guides.

## Phase 3: Verification
- [ ] 3.1 Run `mvn package`
- [ ] 3.2 Run `mvn test`
- [ ] 3.3 Run `mvn verify`
- [ ] 3.4 Produce verification report
