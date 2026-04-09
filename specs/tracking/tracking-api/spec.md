# Feature: tracking-api

Enables host applications to record allowed feature-usage events with minimal integration effort.

## Background

The host application configures the library at startup with a project short tag. Every accepted usage event includes that project short tag.

## Scenarios

### Scenario: Records a tagged feature usage event

* *GIVEN* the library is configured with a project short tag
* *AND* tracking is enabled
* *WHEN* the host application records a feature-usage event with allowed string data
* *THEN* the library SHALL accept the event for delivery
* *AND* the library SHALL add the configured project short tag to the event payload

### Scenario: Rejects unsupported usage payloads

* *GIVEN* the library is configured and tracking is enabled
* *WHEN* the host application records usage data that contains numeric values or unsupported fields
* *THEN* the library SHALL reject the event
* *AND* the library MUST NOT enqueue the rejected payload for delivery
* *AND* the library MUST NOT emit logs, stack traces, or PII as telemetry fields
