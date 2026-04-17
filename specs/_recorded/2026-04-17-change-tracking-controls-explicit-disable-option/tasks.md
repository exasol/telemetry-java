# Tasks: change-tracking-controls-explicit-disable-option

## Phase 2: Implementation
- [x] 2.1 Add a builder-level disable option to `TelemetryConfig` and include it in disabled-state resolution.
- [x] 2.2 Extend the `tracking-controls` spec with a scenario for explicit host-configured disablement.
- [x] 2.3 Extend `status-logging` so the disabled `INFO` message distinguishes host-configured disablement from `EXASOL_TELEMETRY_DISABLE` and `CI`.
- [x] 2.4 Add unit coverage for disabled-state resolution when the explicit option is set alone and alongside environment variables.
- [x] 2.5 Add or extend integration coverage to verify that explicitly disabled telemetry does not emit requests and logs the correct disable mechanism.
- [x] 2.6 Update user-facing docs where configuration-based disablement is described for integrators or end users.

## Phase 3: Verification
- [x] 3.1 Run targeted RED/GREEN tests for changed behavior.
- [x] 3.2 Run `mvn package`.
- [x] 3.3 Run `mvn test`.
- [x] 3.4 Run `mvn verify`.
- [x] 3.5 Audit scenario coverage against the plan.
- [x] 3.6 Produce `verification-report.md`.
