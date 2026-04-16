# Feature: tracking-api

Enables host applications to record allowed feature-usage events with minimal integration effort.

## Requirement: Tracking API
`req~tracking-api~1`

The library shall provide a tracking API that accepts feature-usage events from the host application, queues accepted tracking work for asynchronous delivery, and keeps the caller thread free of delivery work as described by the scenarios below.

Covers:
* `feat~tracking-api~1`

Needs: impl, utest, itest

## Background

The host application configures the library at startup and then records feature-usage events through the telemetry client.

## Scenarios

### Scenario: Records a feature usage event

* *GIVEN* the library is configured
* *AND* tracking is enabled
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL accept the event for delivery

### Scenario: Ignores tracking after the client is closed

* *GIVEN* the host application has closed the telemetry client
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL ignore that call
* *AND* the library MUST NOT enqueue the event for delivery

### Scenario: Keeps caller-thread overhead low for accepted tracking

* *GIVEN* tracking is enabled
* *AND* the host application records a feature-usage event
* *WHEN* the library accepts the event for delivery
* *THEN* the library SHALL keep caller-thread work limited to creating the event, capturing the timestamp, and queue admission
* *AND* the library SHALL defer JSON serialization and HTTP delivery to background processing
* *AND* the library SHOULD avoid avoidable heap allocations on the caller thread

### Scenario: Makes disabled tracking a no-op without telemetry overhead

* *GIVEN* tracking is disabled
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL return without queueing or delivery work
* *AND* the library MUST NOT allocate telemetry event or protocol objects for that call
* *AND* the library MUST NOT perform network or background coordination for that call
