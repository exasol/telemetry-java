# Feature: tracking-api

Enables host applications to record allowed feature-usage events with minimal integration effort.

## Requirement
`req~tracking-api~1`

The library shall provide a tracking API that accepts valid feature-usage events, qualifies emitted feature names with the configured project tag, rejects invalid tracking calls, and keeps accepted tracking work off the caller thread as described by the scenarios below.

Covers:
* `feat~tracking-api~1`

Needs: impl, utest, itest

## Background

The host application configures the library at startup with a project short tag. Every accepted usage event uses that project short tag to namespace the feature name in the telemetry protocol.

## Scenarios

### Scenario: Records a tagged feature usage event

* *GIVEN* the library is configured with a project short tag
* *AND* tracking is enabled
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL accept the event for delivery
* *AND* the library SHALL prefix the feature name with the configured project short tag in the emitted protocol payload

### Scenario: Rejects unsupported usage payloads

* *GIVEN* the library is configured and tracking is enabled
* *WHEN* the host application records usage data that contains unsupported fields
* *THEN* the library SHALL reject the event
* *AND* the library MUST NOT enqueue the rejected payload for delivery
* *AND* the library MUST NOT emit logs, stack traces, or PII in the protocol payload

### Scenario: Rejects tracking after the client is closed

* *GIVEN* the host application has closed the telemetry client
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL report that the client is closed
* *AND* the library MUST NOT enqueue the event for delivery

### Scenario: Keeps caller-thread overhead low for accepted tracking

* *GIVEN* tracking is enabled
* *AND* the host application records a valid feature-usage event
* *WHEN* the library accepts the event for delivery
* *THEN* the library SHALL keep caller-thread work limited to feature validation, feature qualification, timestamp capture, and queue admission
* *AND* the library SHALL defer JSON serialization and HTTP delivery to background processing
* *AND* the library SHOULD avoid avoidable heap allocations on the caller thread

### Scenario: Makes disabled tracking a no-op without telemetry overhead

* *GIVEN* tracking is disabled
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL return without queueing or delivery work
* *AND* the library MUST NOT allocate telemetry event or protocol objects for that call
* *AND* the library MUST NOT perform network or background coordination for that call
