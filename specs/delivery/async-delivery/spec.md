# Feature: async-delivery

Delivers accepted usage events to an HTTP endpoint without blocking the host application's main execution path.

## Requirement: Async Delivery
`req~async-delivery~1`

The library shall batch accepted telemetry events into protocol messages and deliver them asynchronously over HTTP with retry handling and bounded in-memory buffering as described by the scenarios below.

Covers:
* `feat~async-delivery~1`

Needs: impl, utest, itest

## Background

Accepted telemetry events are serialized to JSON and delivered via HTTP `POST` to a configured endpoint or the default endpoint `https://metrics.exasol.com`. The JSON payload contains `category`, protocol `version`, `productVersion`, `timestamp`, and `features` fields, and the library uses bounded in-memory buffering with no persistent local storage.

## Scenarios

### Scenario: Sends queued events asynchronously over HTTP

* *GIVEN* the library is configured with an endpoint, project tag, and `productVersion`
* *AND* an accepted usage event is queued for delivery
* *WHEN* the background sender processes the queue
* *THEN* the library SHALL submit a protocol message as JSON using HTTP `POST`
* *AND* the library SHALL include `category`, `version`, `productVersion`, `timestamp`, and `features` fields in that JSON payload
* *AND* the library SHALL emit protocol `version`=`0.2.0`
* *AND* the library SHALL perform network delivery without blocking the calling thread

### Scenario: Retries failed delivery with exponential backoff until timeout

* *GIVEN* the background sender attempts to deliver a queued event
* *AND* the configured endpoint fails to accept the request
* *WHEN* the delivery attempt fails before the retry timeout expires
* *THEN* the library SHALL retry delivery using exponential backoff
* *AND* the library SHALL stop retrying that event when the retry timeout is reached
* *AND* the library MUST use bounded memory while retrying

### Scenario: Batches multiple drained events into a single protocol message

* *GIVEN* multiple accepted telemetry events are present when the background sender drains the queue
* *WHEN* the background sender emits the next protocol message
* *THEN* the library SHALL include the queued events in a single JSON payload
* *AND* the library SHALL group timestamps by caller-provided feature name in the `features` map
* *AND* the library SHALL correctly JSON-escape caller-provided feature names when serializing the `features` map
