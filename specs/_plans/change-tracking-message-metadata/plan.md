# Plan: change-tracking-message-metadata

## Summary

This plan replaces project-tag-prefixed feature names with explicit top-level message metadata. It adds a permanent config feature for required client identity inputs and updates the tracking, delivery, and tracking-controls specs so they consistently require `category=projectTag`, `version=<host product version>`, and caller-provided feature names in the payload without feature-name validation beyond ignoring `null` values.

## Design

### Context

The current contract mixes two identities into one field by encoding the configured project tag into every feature name, while the issue requires a top-level `category` field and a required host-product version. The permanent spec library also lacks a feature that owns required client identity inputs, so the new runtime contract would remain underspecified unless the plan adds one.

- **Goals** — Move project identity out of feature names, require host applications to configure both project tag and product version, preserve application-chosen feature names without validation apart from ignoring `null` values, and make emitted payloads carry `category` and `version` explicitly.
- **Non-Goals** — Change retry behavior, add extra payload fields, impose feature-name rules, or redesign endpoint/environment override behavior.

### Decision

Add a new permanent `config/client-identity` feature that defines required project tag and version inputs. Update the tracking API so accepted feature names are sent exactly as provided rather than being qualified with the project tag or validated by the library, except that `null` feature names are ignored and not queued. Update async delivery so every JSON payload includes `category`, `version`, `timestamp`, and `features` with correct JSON escaping for feature keys.

#### Architecture

```
┌──────────────────┐
│ TelemetryConfig  │
│ projectTag       │
│ version          │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ TelemetryClient  │
│ track(feature)   │
│ no tag prefixing │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Message          │
│ category         │
│ version          │
│ timestamp        │
│ features         │
└──────────────────┘
```

### Consequences

| Decision | Alternatives Considered | Rationale |
|----------|------------------------|-----------|
| Add `config/client-identity` as a new permanent feature | Fold version requirements into `tracking-api` only | Required configuration inputs are part of the public setup contract and need a clear permanent owner |
| Send raw feature names and add top-level `category` | Keep prefixing features and also add `category` | Prefixing would duplicate project identity and violate the issue acceptance criteria |
| Define `version` as the configured host product/library version | Keep the existing hard-coded protocol version field and add another field | The issue explicitly wants emitted version information about the integrating product/library |

## Features

| Feature | Status | Spec |
|---------|--------|------|
| client-identity | NEW | `config/client-identity/spec.md` |
| tracking-controls | CHANGED | `config/tracking-controls/spec.md` |
| async-delivery | CHANGED | `delivery/async-delivery/spec.md` |
| tracking-api | CHANGED | `tracking/tracking-api/spec.md` |

## Requirements

| Requirement | Details |
|-------------|---------|
| Required identity inputs | Host applications SHALL provide a non-blank project tag and a non-blank product/library version when creating telemetry configuration |
| Payload identity | Emitted telemetry messages SHALL include `category` equal to the configured project tag |
| Payload version | Emitted telemetry messages SHALL include `version` equal to the configured host product/library version |
| Feature semantics | Emitted `features` keys SHALL use the caller-provided feature names without adding the project tag as a prefix or validating the chosen names, except that `null` feature names SHALL be ignored |
| JSON encoding | The library SHALL correctly JSON-escape caller-provided feature names when serializing the `features` map |

## Implementation Tasks

1. Extend `TelemetryConfig` to require and expose both project tag and product/library version through the public builder entry point.
2. Remove feature-name qualification and feature-name validation from `TelemetryClient` so it queues caller-provided feature names without project-tag prefixing or library-side filtering, while ignoring `null` feature names.
3. Update `Message` serialization to emit `category`, configured `version`, `timestamp`, and caller-provided feature keys with correct JSON escaping.
4. Adjust helper/test fixtures such as `RecordingHttpServer` and any hard-coded payload assertions to use the new builder contract and payload shape.
5. Update docs and Javadocs where they still describe project-tag-prefixed feature names or a fixed payload version.

## Dead Code Removal

| Type | Location | Reason |
|------|----------|--------|
| Field | `src/main/java/com/exasol/telemetry/TelemetryClient.java` | The cached feature-prefix field becomes obsolete once feature qualification is removed |
| Methods | `src/main/java/com/exasol/telemetry/TelemetryClient.java` | Feature qualification and most feature-name validation helpers become obsolete once raw feature names are emitted without validation apart from ignoring `null` |
| Test assertions | `src/test/java/com/exasol/telemetry/*` | Assertions expecting prefixed feature keys or fixed payload version strings become obsolete under the new payload contract |

## Verification

### Scenario Coverage

| Scenario | Test Type | Test Location | Test Name |
|----------|-----------|---------------|-----------|
| Requires project tag and version when creating telemetry configuration | Unit | `src/test/java/com/exasol/telemetry/TelemetryConfigTest.java` | `requiresProjectTagAndVersion` |
| Overrides the configured endpoint via environment variable | Integration | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `overridesConfiguredEndpointWithoutChangingPayloadIdentity` |
| Sends queued events asynchronously over HTTP with configured identity metadata | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `recordsFeatureUsageEventWithCategoryAndVersion` |
| Batches multiple drained events into a single protocol message | Integration | `src/test/java/com/exasol/telemetry/AsyncDeliveryIT.java` | `batchesMultipleDrainedEventsIntoSingleProtocolMessage` |
| Records a feature usage event without project-tag prefixing or feature-name validation | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `recordsFeatureUsageEventWithoutPrefixingOrValidation` |
| Ignores null feature names | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `ignoresNullFeatureNames` |
| Escapes feature names correctly in JSON | Unit | `src/test/java/com/exasol/telemetry/MessageTest.java` | `escapesFeatureNamesInJson` |
| Keeps caller-thread overhead low for accepted tracking | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `keepsCallerThreadOverheadLowForAcceptedTracking` |

### Manual Testing

| Feature | Command | Expected Output |
|---------|---------|-----------------|
| client-identity, tracking-controls, async-delivery, tracking-api | `mvn verify` | Tests confirm required config identity, unvalidated caller-provided feature names except ignored `null` values, correct JSON escaping, top-level `category`, and configured `version` across payload-producing paths |

### Checklist

| Step | Command | Expected |
|------|---------|----------|
| Build | `mvn package` | Exit 0 |
| Test | `mvn test` | 0 failures |
| Lint | `mvn verify` | Exit 0 |
| Format | `mvn verify` | Exit 0; no dedicated formatter command is defined in the mission |
