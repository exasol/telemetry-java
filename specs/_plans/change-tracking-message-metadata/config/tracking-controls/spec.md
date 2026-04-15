# Feature: tracking-controls

Preserves environment-based tracking control behavior while keeping message identity metadata stable.

## Background

Environment variables may disable telemetry or override the delivery endpoint, but they do not redefine the configured project identity attached to emitted messages.

## Scenarios

<!-- DELTA:CHANGED -->
### Scenario: Overrides the configured endpoint via environment variable

* *GIVEN* the host application configures an endpoint, project tag, and product/library version in code
* *AND* the host environment defines an endpoint override
* *WHEN* the library initializes
* *THEN* the library SHALL use the environment-provided endpoint for delivery
* *AND* the library SHALL continue to emit the configured project tag as the `category` field
* *AND* the library SHALL continue to emit the configured product/library version as the `version` field
<!-- /DELTA:CHANGED -->
