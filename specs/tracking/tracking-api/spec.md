# Feature: tracking-api

Enables host applications to record allowed feature-usage events with minimal integration effort.

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
