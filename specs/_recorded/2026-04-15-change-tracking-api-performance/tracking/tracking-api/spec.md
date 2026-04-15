# Feature: tracking-api

Enables host applications to record allowed feature-usage events with minimal integration effort.

## Background

The host application configures the library at startup with a project short tag. Every accepted usage event uses that project short tag to namespace the feature name in the telemetry protocol.

## Scenarios

<!-- DELTA:NEW -->
### Scenario: Keeps caller-thread overhead low for accepted tracking

* *GIVEN* tracking is enabled
* *AND* the host application records a valid feature-usage event
* *WHEN* the library accepts the event for delivery
* *THEN* the library SHALL keep caller-thread work limited to feature validation, feature qualification, timestamp capture, and queue admission
* *AND* the library SHALL defer JSON serialization and HTTP delivery to background processing
* *AND* the library SHOULD avoid avoidable heap allocations on the caller thread
<!-- /DELTA:NEW -->

<!-- DELTA:NEW -->
### Scenario: Makes disabled tracking a no-op without telemetry overhead

* *GIVEN* tracking is disabled
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL return without queueing or delivery work
* *AND* the library MUST NOT allocate telemetry event or protocol objects for that call
* *AND* the library MUST NOT perform network or background coordination for that call
<!-- /DELTA:NEW -->
