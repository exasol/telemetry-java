# Feature: async-delivery

Aligns the async delivery contract with the implemented queue-drain batching behavior.

## Background

The sender already groups events that are drained together into a single protocol payload. This delta records that batching behavior in the feature contract.

## Scenarios

<!-- DELTA:NEW -->
### Scenario: Batches multiple drained events into a single protocol message

* *GIVEN* multiple accepted telemetry events are present when the background sender drains the queue
* *WHEN* the background sender emits the next protocol message
* *THEN* the library SHALL include the queued events in a single JSON payload
* *AND* the library SHALL group timestamps by fully qualified feature name in the `features` map
<!-- /DELTA:NEW -->
