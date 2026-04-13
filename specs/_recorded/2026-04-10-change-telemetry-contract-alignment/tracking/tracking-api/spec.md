# Feature: tracking-api

Aligns the tracking API contract with the implemented post-close behavior.

## Background

The telemetry client already rejects tracking calls after the client is closed. This delta records that behavior in the feature contract.

## Scenarios

<!-- DELTA:NEW -->
### Scenario: Rejects tracking after the client is closed

* *GIVEN* the host application has closed the telemetry client
* *WHEN* the host application records a feature-usage event
* *THEN* the library SHALL report that the client is closed
* *AND* the library MUST NOT enqueue the event for delivery
<!-- /DELTA:NEW -->
