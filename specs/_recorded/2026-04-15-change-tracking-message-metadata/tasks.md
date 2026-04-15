# Tasks: change-tracking-message-metadata

## Phase 2: Implementation
- [x] 2.1 Update telemetry configuration and payload model to require project tag and `productVersion`, keep protocol `version`, and emit `category`, `version=0.2.0`, and `productVersion`
- [x] 2.2 Remove feature-name prefixing, ignore `null` feature names, and preserve caller-provided feature names for delivery
- [x] 2.3 Update unit and integration tests plus helper fixtures for the new builder contract and payload shape
- [x] 2.4 Update docs and Javadocs that still describe prefixed feature names or outdated payload field semantics

## Phase 3: Verification
- [x] 3.1 Run targeted tests for configuration, payload serialization, transport, tracking API, delivery, and tracking controls
- [x] 3.2 Run `mvn test`
- [x] 3.3 Run `mvn verify`
- [x] 3.4 Generate verification report
