# Plan: change-tracking-api-performance

## Summary

This plan adds performance-oriented requirements to the tracking API contract. It specifies that accepted tracking calls must keep caller-thread overhead low and that disabled tracking must behave as a no-op without telemetry work or per-call telemetry object allocation.

## Design

### Context

The mission emphasizes minimal integration effort, easy auditing, bounded memory usage, and avoiding unnecessary runtime cost. The current specs define the functional behavior of `track(...)`, but they do not yet constrain hot-path overhead or make the disabled path explicitly allocation-free.

- **Goals** — Keep the public tracking API lightweight on the caller thread, clarify that serialization and transport stay off the hot path, and require disabled tracking to be a no-op without telemetry overhead.
- **Non-Goals** — Introduce benchmark-specific SLAs, guarantee zero JVM allocations in all circumstances, or redesign delivery architecture beyond the existing queue and background sender model.

### Decision

Extend the existing `tracking-api` feature with performance requirements instead of creating a new cross-cutting feature. The `track(...)` contract will state that accepted calls are limited to minimal caller-thread work and that disabled calls do not create telemetry work items or protocol objects.

#### Architecture

```
track(feature)
    ├─ validate and qualify feature
    ├─ capture timestamp
    ├─ enqueue event when enabled
    └─ return immediately

background sender
    ├─ serialize JSON
    └─ perform HTTP delivery
```

#### Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| Hot-path minimization | `TelemetryClient.track(...)` | Keeps application-facing overhead low during normal usage |
| Deferred work | Queue and background sender | Preserves asynchronous delivery and keeps serialization and I/O off the caller thread |
| Fast no-op guard | Disabled tracking path | Prevents telemetry from affecting disabled deployments and tests |

### Consequences

| Decision | Alternatives Considered | Rationale |
|----------|------------------------|-----------|
| Add performance constraints to `tracking-api` | Create a separate `performance` feature | The requirements refine the existing `track(...)` contract rather than introducing a standalone capability |
| Specify qualitative overhead constraints | Add fixed latency/allocation thresholds now | The library needs enforceable behavior without brittle, environment-specific benchmark numbers in the spec |
| Require disabled-path no-op semantics | Only require “no delivery” when disabled | The user asked for no-op behavior without overhead, so the spec must constrain hot-path work as well |

## Features

| Feature | Status | Spec |
|---------|--------|------|
| tracking-api | CHANGED | `tracking/tracking-api/spec.md` |

## Requirements

| Requirement | Details |
|-------------|---------|
| Low caller-thread overhead | Accepted tracking calls SHALL limit caller-thread work to admission steps and defer delivery work |
| Disabled-path no-op | Disabled tracking calls SHALL return without queueing, delivery work, or per-call telemetry object creation |

## Implementation Tasks

1. Add spec coverage for low-overhead accepted tracking and disabled-path no-op behavior in `tracking-api`.
2. Add integration tests that verify accepted calls remain asynchronous and disabled calls do not produce telemetry work.
3. Review the tracking hot path and remove avoidable allocations or work on the disabled and accepted caller-thread paths.
4. Update integration-facing documentation if the performance and disabled-path guarantees need to be communicated explicitly.

## Dead Code Removal

| Type | Location | Reason |
|------|----------|--------|
| None identified | n/a | This change tightens behavior requirements but does not currently imply obsolete code |

## Verification

### Scenario Coverage

| Scenario | Test Type | Test Location | Test Name |
|----------|-----------|---------------|-----------|
| Keeps caller-thread overhead low for accepted tracking | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `keepsCallerThreadOverheadLowForAcceptedTracking` |
| Makes disabled tracking a no-op without telemetry overhead | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `makesDisabledTrackingNoOpWithoutTelemetryOverhead` |

### Manual Testing

| Feature | Command | Expected Output |
|---------|---------|-----------------|
| tracking-api | `mvn verify` | Performance-oriented tracking tests pass and the tracking API still behaves asynchronously with disabled calls producing no telemetry work |

### Checklist

| Step | Command | Expected |
|------|---------|----------|
| Build | `mvn package` | Exit 0 |
| Test | `mvn test` | 0 failures |
| Lint | `mvn verify` | Exit 0 |
| Format | `mvn verify` | Exit 0; no dedicated formatter command is defined in the mission |
