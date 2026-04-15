# Plan: add-telemetry-library-mvp

## Summary

This plan defines the MVP for `telemetry-java` as a minimal Java 11 telemetry library that host applications can integrate with low effort to enqueue feature-usage events and deliver them asynchronously over HTTP. The MVP covers configuration, JSON delivery, bounded buffering, exponential-backoff retry with a timeout, opt-out behavior, and clean shutdown semantics.

## Design

### Context

The library needs to provide useful feature-usage telemetry without adopting the breadth, weight, or data-collection scope of larger telemetry SDKs. The design must preserve zero dependencies, bounded resource usage, easy auditability, and a straightforward host integration model.

- **Goals** — Provide a small integration surface, send only allowed telemetry fields, avoid blocking host application threads, and flush pending telemetry on shutdown.
- **Non-Goals** — Collect logs, stack traces, high-frequency data, numeric data, persistent local storage, or personally identifiable information.

### Decision

The MVP is structured around a small public tracking API that validates and normalizes usage events, appends a required project short tag, and enqueues events into a bounded in-memory buffer. A dedicated background sender drains the queue, serializes events to JSON, and submits them via HTTP `POST` to a configured endpoint with exponential backoff until success or retry-timeout expiry; the library exposes `AutoCloseable` shutdown semantics to flush pending work and stop sender threads.

#### Architecture

```text
┌────────────────┐     ┌──────────────────┐     ┌──────────────────┐     ┌────────────────┐
│ Host app code  │────▶│ Tracking API     │────▶│ Bounded queue    │────▶│ Background     │
│                │     │ + validation     │     │ / buffer         │     │ sender         │
└────────────────┘     └──────────────────┘     └──────────────────┘     └──────┬─────────┘
                                                                                 │
                                                                                 ▼
                                                                        ┌────────────────┐
                                                                        │ HTTP POST JSON │
                                                                        │ configured URL │
                                                                        └────────────────┘
```

#### Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| Facade | Public tracking API | Keeps host integration small and auditable |
| Producer-consumer | Queue and background sender | Decouples event recording from network I/O |
| Bounded buffer | In-memory queue | Prevents unbounded memory growth |
| Retry with exponential backoff | HTTP delivery loop | Improves resiliency without blocking callers |
| Lifecycle via `AutoCloseable` | Library client/session | Gives host apps explicit shutdown control |

### Consequences

| Decision | Alternatives Considered | Rationale |
|----------|------------------------|-----------|
| Use an in-memory bounded queue | Synchronous sends, persistent spool | Keeps runtime simple, non-blocking, and dependency-free |
| Use a dedicated background sender | Caller-managed send loop | Preserves easy integration and isolates transport concerns |
| Stop retries after a timeout | Retry forever, fixed retry count only | Prevents stuck background work and unbounded shutdown delays |
| Configure endpoint override via environment variable | Code-only configuration | Supports deployment overrides without app rebuilds |
| Require a project short tag at startup | Infer tag from runtime, make tag optional | Ensures all events carry a stable application identifier |

## Features

| Feature | Status | Spec |
|---------|--------|------|
| tracking-api | NEW | `tracking/tracking-api/spec.md` |
| async-delivery | NEW | `delivery/async-delivery/spec.md` |
| shutdown-flush | NEW | `lifecycle/shutdown-flush/spec.md` |
| tracking-controls | NEW | `config/tracking-controls/spec.md` |

## Requirements

| Requirement | Details |
|-------------|---------|
| Minimal integration | Host applications configure the library at startup with a project short tag and an endpoint |
| Data minimization | Telemetry payloads only contain permitted string-based usage fields and MUST NOT include PII |
| Runtime safety | Sending MUST NOT block the main application thread and MUST use bounded memory |
| Lifecycle correctness | Closing the library MUST flush queued telemetry and stop background threads |

## Dependencies

- Java 11 runtime
- Maven build
- JUnit Jupiter for automated tests
- JaCoCo and Sonar in the Maven quality pipeline

## Implementation Tasks

1. Define the public configuration and tracking API, including required project short tag and environment-variable overrides.
2. Implement event validation and JSON payload shaping for allowed telemetry fields only.
3. Implement a bounded in-memory queue and background sender that performs HTTP `POST` delivery.
4. Implement retry handling with exponential backoff and retry-timeout enforcement.
5. Implement `AutoCloseable` shutdown logic that drains pending events and terminates background threads.
6. Add Maven integration tests that cover all scenarios with a controllable local HTTP test endpoint.
7. Document startup configuration and shutdown expectations in the developer and app user guides.

## Parallelization

| Parallel Group | Tasks |
|----------------|-------|
| Group A | Task 1, Task 2 |
| Group B | Task 3, Task 4 |
| Group C | Task 5, Task 6 |
| Group D | Task 7 |

Sequential dependencies:
- Group A → Group B
- Group B → Group C
- Group C → Group D

## Dead Code Removal

| Type | Location | Reason |
|------|----------|--------|
| None | N/A | Greenfield MVP plan; no obsolete code exists yet |

## Verification

### Scenario Coverage

| Scenario | Test Type | Test Location | Test Name |
|----------|-----------|---------------|-----------|
| Records a tagged feature usage event | Integration | `src/test/java/.../TrackingApiIT.java` | `recordsTaggedFeatureUsageEvent` |
| Rejects unsupported usage payloads | Integration | `src/test/java/.../TrackingApiIT.java` | `rejectsUnsupportedUsagePayloads` |
| Sends queued events asynchronously over HTTP | Integration | `src/test/java/.../AsyncDeliveryIT.java` | `sendsQueuedEventsAsynchronouslyOverHttp` |
| Retries failed delivery with exponential backoff until timeout | Integration | `src/test/java/.../AsyncDeliveryIT.java` | `retriesFailedDeliveryWithExponentialBackoffUntilTimeout` |
| Flushes pending events on close | Integration | `src/test/java/.../ShutdownFlushIT.java` | `flushesPendingEventsOnClose` |
| Stops background threads after close | Integration | `src/test/java/.../ShutdownFlushIT.java` | `stopsBackgroundThreadsAfterClose` |
| Disables tracking via environment variables | Integration | `src/test/java/.../TrackingControlsIT.java` | `disablesTrackingViaEnvironmentVariables` |
| Overrides the configured endpoint via environment variable | Integration | `src/test/java/.../TrackingControlsIT.java` | `overridesConfiguredEndpointViaEnvironmentVariable` |

### Manual Testing

| Feature | Command | Expected Output |
|---------|---------|-----------------|
| tracking-api | `mvn test` | Integration tests confirm accepted and rejected tracking calls as specified |
| async-delivery | `mvn test` | Integration tests confirm background HTTP delivery and retry behavior |
| shutdown-flush | `mvn test` | Integration tests confirm close flushes pending telemetry and stops worker threads |
| tracking-controls | `mvn test` | Integration tests confirm environment-variable disablement and endpoint override behavior |

### Checklist

| Step | Command | Expected |
|------|---------|----------|
| Build | `mvn package` | Exit 0 |
| Test | `mvn test` | 0 failures |
| Lint | `mvn verify` | Exit 0 with JaCoCo/Sonar-integrated quality checks configured |
| Format | `mvn verify` | Exit 0; no dedicated formatter command is defined in the mission |
