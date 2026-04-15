# Feature: tracking-api

Enables host applications to record feature-usage events without coupling feature names to the configured project tag.

## Background

The host application still configures project identity once at startup, but accepted feature names are transmitted as caller-provided strings. Project tag and product version are emitted separately as message metadata, and the library does not validate which feature names applications choose apart from ignoring `null` values.

## Scenarios

<!-- DELTA:CHANGED -->
### Scenario: Records a tagged feature usage event

* *GIVEN* the library is configured with a project short tag and product/library version
* *AND* tracking is enabled
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL accept the event for delivery
* *AND* the library SHALL preserve the caller-provided feature name in the emitted protocol payload without adding the configured project short tag as a prefix
* *AND* the library MUST NOT validate or reject the feature name based on application-defined naming choices
<!-- /DELTA:CHANGED -->

<!-- DELTA:NEW -->
### Scenario: Ignores null feature names

* *GIVEN* tracking is enabled
* *WHEN* the host application records a `null` feature name
* *THEN* the library SHALL ignore that call
* *AND* the library MUST NOT enqueue or emit a telemetry event for the `null` feature name
<!-- /DELTA:NEW -->

<!-- DELTA:CHANGED -->
### Scenario: Keeps caller-thread overhead low for accepted tracking

* *GIVEN* tracking is enabled
* *AND* the host application records a valid feature-usage event
* *WHEN* the library accepts the event for delivery
* *THEN* the library SHALL keep caller-thread work limited to receiving the feature name, timestamp capture, and queue admission
* *AND* the library SHALL defer JSON serialization and HTTP delivery to background processing
* *AND* the library SHOULD avoid avoidable heap allocations on the caller thread
<!-- /DELTA:CHANGED -->
