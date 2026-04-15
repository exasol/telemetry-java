# Plan: change-tracking-message-metadata

## Summary

This plan replaces project-tag-prefixed feature names with explicit top-level message metadata. It keeps the JSON `version` field as the telemetry protocol version, increments that protocol version to `0.2.0`, and adds a separate `productVersion` field for the host product/library version. It also updates the tracking, delivery, and tracking-controls specs so they consistently require caller-provided feature names in the payload without feature-name validation beyond ignoring `null` values.

## Design

### Context

The current contract mixes two identities into one field by encoding the configured project tag into every feature name, while the issue requires a top-level `category` field and a required host product/library version. The JSON payload already has a `version` field, so changing its meaning would break the protocol contract unless the host-side version is given a distinct name. The permanent spec library also lacks a feature that owns required client identity inputs, so the new runtime contract would remain underspecified unless the plan adds one.

- **Goals** — Move project identity out of feature names, require host applications to configure both project tag and product/library version, preserve application-chosen feature names without validation apart from ignoring `null` values, keep the protocol `version` field, and make emitted payloads carry `category` and `productVersion` explicitly.
- **Non-Goals** — Change retry behavior, add extra payload fields, impose feature-name rules, or redesign endpoint/environment override behavior.

### Decision

Add a new permanent `config/client-identity` feature that defines required project tag and `productVersion` inputs. Keep the protocol `version` field in the JSON message and increment it to `0.2.0`. Update the tracking API so accepted feature names are sent exactly as provided rather than being qualified with the project tag or validated by the library, except that `null` feature names are ignored and not queued. Update async delivery so every JSON payload includes `category`, `version`, `productVersion`, `timestamp`, and `features` with correct JSON escaping for feature keys.

#### Architecture

```
┌──────────────────┐
│ TelemetryConfig  │
│ projectTag       │
│ productVersion   │
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
│ version=0.2.0    │
│ productVersion   │
│ timestamp        │
│ features         │
└──────────────────┘
```

### Consequences

| Decision | Alternatives Considered | Rationale |
|----------|------------------------|-----------|
| Add `config/client-identity` as a new permanent feature | Fold `productVersion` requirements into `tracking-api` only | Required configuration inputs are part of the public setup contract and need a clear permanent owner |
| Send raw feature names and add top-level `category` | Keep prefixing features and also add `category` | Prefixing would duplicate project identity and violate the issue acceptance criteria |
| Keep `version` as the protocol version and add `productVersion` for the host product/library version | Reinterpret `version` as the host version | Preserves the existing protocol field semantics and avoids overloading one field with two meanings |

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
| Required identity inputs | Host applications SHALL provide a non-blank project tag and a non-blank `productVersion` when creating telemetry configuration |
| Payload identity | Emitted telemetry messages SHALL include `category` equal to the configured project tag |
| Protocol version | Emitted telemetry messages SHALL include `version` equal to the protocol version `0.2.0` |
| Product version | Emitted telemetry messages SHALL include `productVersion` equal to the configured host product/library version |
| Feature semantics | Emitted `features` keys SHALL use the caller-provided feature names without adding the project tag as a prefix or validating the chosen names, except that `null` feature names SHALL be ignored |
| JSON encoding | The library SHALL correctly JSON-escape caller-provided feature names when serializing the `features` map |

## Implementation Tasks

1. Extend `TelemetryConfig` to require and expose both project tag and `productVersion` through the public builder entry point.
2. Remove feature-name qualification and feature-name validation from `TelemetryClient` so it queues caller-provided feature names without project-tag prefixing or library-side filtering, while ignoring `null` feature names.
3. Update `Message` serialization to emit `category`, protocol `version`=`0.2.0`, configured `productVersion`, `timestamp`, and caller-provided feature keys with correct JSON escaping.
4. Adjust helper/test fixtures such as `RecordingHttpServer` and any hard-coded payload assertions to use the new builder contract and payload shape.
5. Update docs and Javadocs where they still describe project-tag-prefixed feature names or outdated payload field semantics.

## Dead Code Removal

| Type | Location | Reason |
|------|----------|--------|
| Field | `src/main/java/com/exasol/telemetry/TelemetryClient.java` | The cached feature-prefix field becomes obsolete once feature qualification is removed |
| Methods | `src/main/java/com/exasol/telemetry/TelemetryClient.java` | Feature qualification and most feature-name validation helpers become obsolete once raw feature names are emitted without validation apart from ignoring `null` |
| Test assertions | `src/test/java/com/exasol/telemetry/*` | Assertions expecting prefixed feature keys or the old protocol version string become obsolete under the new payload contract |

## Verification

### Scenario Coverage

| Scenario | Test Type | Test Location | Test Name |
|----------|-----------|---------------|-----------|
| Requires project tag and productVersion when creating telemetry configuration | Unit | `src/test/java/com/exasol/telemetry/TelemetryConfigTest.java` | `requiresProjectTagAndProductVersion` |
| Overrides the configured endpoint via environment variable | Integration | `src/test/java/com/exasol/telemetry/TrackingControlsIT.java` | `overridesConfiguredEndpointWithoutChangingPayloadIdentity` |
| Sends queued events asynchronously over HTTP with configured identity metadata | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `recordsFeatureUsageEventWithCategoryProtocolVersionAndProductVersion` |
| Batches multiple drained events into a single protocol message | Integration | `src/test/java/com/exasol/telemetry/AsyncDeliveryIT.java` | `batchesMultipleDrainedEventsIntoSingleProtocolMessage` |
| Records a feature usage event without project-tag prefixing or feature-name validation | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `recordsFeatureUsageEventWithoutPrefixingOrValidation` |
| Ignores null feature names | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `ignoresNullFeatureNames` |
| Emits protocol version `0.2.0`, configured `productVersion`, and escaped feature names in JSON | Unit | `src/test/java/com/exasol/telemetry/MessageTest.java` | `serializesProtocolVersionProductVersionAndEscapedFeatureNames` |
| Keeps caller-thread overhead low for accepted tracking | Integration | `src/test/java/com/exasol/telemetry/TrackingApiIT.java` | `keepsCallerThreadOverheadLowForAcceptedTracking` |

### Manual Testing

| Feature | Command | Expected Output |
|---------|---------|-----------------|
| client-identity, tracking-controls, async-delivery, tracking-api | `mvn verify` | Tests confirm required config identity, protocol `version=0.2.0`, configured `productVersion`, unvalidated caller-provided feature names except ignored `null` values, correct JSON escaping, and top-level `category` across payload-producing paths |

### Checklist

| Step | Command | Expected |
|------|---------|----------|
| Build | `mvn package` | Exit 0 |
| Test | `mvn test` | 0 failures |
| Lint | `mvn verify` | Exit 0 |
| Format | `mvn verify` | Exit 0; no dedicated formatter command is defined in the mission |
