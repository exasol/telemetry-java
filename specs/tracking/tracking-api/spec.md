# Feature: tracking-api

Enables host applications to record allowed feature-usage events with minimal integration effort.

## Background

The host application still configures project identity once at startup, but accepted feature names are transmitted as caller-provided strings. Project tag and `productVersion` are emitted separately as message metadata, while the JSON `version` field remains reserved for the telemetry protocol version. The library does not validate which feature names applications choose apart from ignoring `null` values.

## Scenarios

### Scenario: Records a tagged feature usage event

* *GIVEN* the library is configured with a project short tag and `productVersion`
* *AND* tracking is enabled
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL accept the event for delivery
* *AND* the library SHALL preserve the caller-provided feature name in the emitted protocol payload without adding the configured project short tag as a prefix
* *AND* the library SHALL emit the configured `productVersion` as the `productVersion` field
* *AND* the library SHALL keep `version` reserved for protocol version `0.2.0`
* *AND* the library MUST NOT validate or reject the feature name based on application-defined naming choices

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
* *THEN* the library SHALL keep caller-thread work limited to receiving the feature name, timestamp capture, and queue admission
* *AND* the library SHALL defer JSON serialization and HTTP delivery to background processing
* *AND* the library SHOULD avoid avoidable heap allocations on the caller thread

### Scenario: Ignores null feature names

* *GIVEN* tracking is enabled
* *WHEN* the host application records a `null` feature name
* *THEN* the library SHALL ignore that call
* *AND* the library MUST NOT enqueue or emit a telemetry event for the `null` feature name

### Scenario: Makes disabled tracking a no-op without telemetry overhead

* *GIVEN* tracking is disabled
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL return without queueing or delivery work
* *AND* the library MUST NOT allocate telemetry event or protocol objects for that call
* *AND* the library MUST NOT perform network or background coordination for that call
