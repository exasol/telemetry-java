# Verification Report: change-tracking-controls-explicit-disable-option

**Generated:** 2026-04-17

## Verdict

| Result | Details |
|--------|---------|
| **FAIL** | The explicit host-disable feature, disabled log message update, targeted tests, `mvn test`, and OpenFastTrace trace all passed, but the checklist commands `mvn package` and `mvn verify` fail on an existing `project-keeper` validation issue for `.settings/org.eclipse.jdt.core.prefs`. |

| Check | Status |
|-------|--------|
| Build | ✗ |
| Tests | ✓ |
| Lint | ✗ |
| Format | ✗ |
| Scenario Coverage | ✓ |
| Manual Tests | ✗ |

## Test Evidence

### Coverage

| Type | Coverage % |
|------|------------|
| Unit | N/A |
| Integration | N/A |

### Test Results

| Type | Run | Passed | Ignored |
|------|-----|--------|---------|
| Unit + Integration | `mvn -Dtest=TelemetryConfigTest,TrackingControlsIT,StatusLoggingIT test` | 20 | 0 |
| Unit | `mvn test` | 27 | 0 |
| Traceability | `mvn generate-sources org.itsallcode:openfasttrace-maven-plugin:trace` | 60 traced items, 0 defects | 0 |

### Manual Tests

| Test | Result |
|------|--------|
| `mvn verify` for `tracking-controls` and `status-logging` | ✗ |

## Tool Evidence

### Linter

```text
mvn verify
[ERROR] E-PK-CORE-18: Outdated content: '.settings/org.eclipse.jdt.core.prefs'
[ERROR] E-PK-CORE-6: This projects structure does not conform with the template. Run mvn project-keeper:fix to fix the issues automatically.
```

### Formatter

```text
No dedicated formatter command is defined in the mission. The checklist maps format to `mvn verify`, which fails on the same project-keeper validation issue.
```

## Scenario Coverage

| Domain | Feature | Scenario | Test Location | Test Name | Passes |
|--------|---------|----------|---------------|-----------|--------|
| config | tracking-controls | Disables tracking via explicit host configuration | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `disablesTrackingViaExplicitConfiguration` | Pass |
| config | tracking-controls | Disables tracking via environment variables | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `disablesTrackingViaEnvironmentVariables` | Pass |
| config | tracking-controls | Disables tracking automatically in CI | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `disablesTrackingAutomaticallyWhenCiIsNonEmpty` | Pass |
| config | tracking-controls | Overrides the configured endpoint via environment variable | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `overridesConfiguredEndpointViaEnvironmentVariable` | Pass |
| lifecycle | status-logging | Logs when telemetry is disabled | `src/test/java/com/exasol/telemetry/StatusLoggingIT.java` | `logsWhenTelemetryIsDisabledWithMechanism` | Pass |

## Notes

- `TelemetryConfig.Builder.disableTracking()` was added as the explicit host-controlled opt-out API.
- Disabled logging now emits `Telemetry is disabled via host configuration.` when telemetry is disabled in code and still prefers `EXASOL_TELEMETRY_DISABLE` or `CI` when those environment variables are set.
- An initial `mvn test` run failed because stale build outputs left `TelemetryEventTest` unresolved; `mvn clean test` and a subsequent plain `mvn test` both passed, so the current workspace state is consistent.
- The remaining verification blocker is external to this change set: `project-keeper` rejects `.settings/org.eclipse.jdt.core.prefs` during `mvn package` and `mvn verify`.
