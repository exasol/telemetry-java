# Feature: async-delivery

Delivers accepted usage events to a configured HTTP endpoint without blocking the host application's main execution path.

## Background

Accepted telemetry events are serialized to JSON and delivered via HTTP `POST` to a configured endpoint. The library uses bounded in-memory buffering and no persistent local storage.

## Scenarios

### Scenario: Sends queued events asynchronously over HTTP

* *GIVEN* the library is configured with an endpoint
* *AND* an accepted usage event is queued for delivery
* *WHEN* the background sender processes the queue
* *THEN* the library SHALL submit the event as JSON using HTTP `POST`
* *AND* the library SHALL perform network delivery without blocking the calling thread

### Scenario: Retries failed delivery with exponential backoff until timeout

* *GIVEN* the background sender attempts to deliver a queued event
* *AND* the configured endpoint fails to accept the request
* *WHEN* the delivery attempt fails before the retry timeout expires
* *THEN* the library SHALL retry delivery using exponential backoff
* *AND* the library SHALL stop retrying that event when the retry timeout is reached
* *AND* the library MUST use bounded memory while retrying
