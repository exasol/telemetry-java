# Feature: tracking-api

Enables host applications to record allowed feature-usage events with minimal integration effort through a single fire-and-forget tracking call.

## Background

The host application configures the library at startup with a project short tag. Every enqueued usage event uses that project short tag to namespace the feature name in the telemetry protocol.

## Scenarios

### Scenario: Records a tagged feature usage event

* *GIVEN* the library is configured with a project short tag
* *AND* tracking is enabled
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL enqueue the event for delivery
* *AND* the library SHALL prefix the feature name with the configured project short tag in the emitted protocol payload

### Scenario: Ignores invalid feature names

* *GIVEN* the library is configured and tracking is enabled
* *WHEN* the host application records a feature-usage event with an invalid feature name
* *THEN* the library SHALL ignore the call
* *AND* the library MUST NOT enqueue an event for delivery
* *AND* the library MUST NOT throw an exception

### Scenario: Ignores tracking after the client is closed

* *GIVEN* the host application has closed the telemetry client
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL ignore the call
* *AND* the library MUST NOT enqueue the event for delivery
* *AND* the library MUST NOT throw an exception
