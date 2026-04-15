# Feature: async-delivery

Delivers accepted usage events asynchronously while serializing explicit client identity metadata in each payload.

## Background

The telemetry protocol now carries project identity in top-level message fields instead of encoding it into feature names. Async delivery remains responsible for emitting valid JSON over HTTP without blocking the caller thread, including correct escaping of caller-provided feature names in JSON object keys.

## Scenarios

<!-- DELTA:CHANGED -->
### Scenario: Sends queued events asynchronously over HTTP

* *GIVEN* the library is configured with an endpoint, project tag, and product/library version
* *AND* an accepted usage event is queued for delivery
* *WHEN* the background sender processes the queue
* *THEN* the library SHALL submit a protocol message as JSON using HTTP `POST`
* *AND* the library SHALL include `category`, `version`, `timestamp`, and `features` fields in that JSON payload
* *AND* the library SHALL perform network delivery without blocking the calling thread
<!-- /DELTA:CHANGED -->

<!-- DELTA:CHANGED -->
### Scenario: Batches multiple drained events into a single protocol message

* *GIVEN* multiple accepted telemetry events are present when the background sender drains the queue
* *WHEN* the background sender emits the next protocol message
* *THEN* the library SHALL include the queued events in a single JSON payload
* *AND* the library SHALL group timestamps by caller-provided feature name in the `features` map
* *AND* the library SHALL correctly JSON-escape caller-provided feature names when serializing the `features` map
<!-- /DELTA:CHANGED -->
