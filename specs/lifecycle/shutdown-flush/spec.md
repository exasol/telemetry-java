# Feature: shutdown-flush

Ensures host applications can shut down cleanly while still giving queued telemetry an opportunity to be delivered.

## Background

The telemetry client participates in application shutdown through `AutoCloseable`.

## Scenarios

### Scenario: Flushes pending events on close

* *GIVEN* one or more accepted telemetry events remain queued
* *WHEN* the host application closes the telemetry client
* *THEN* the library SHALL attempt to deliver all queued events before shutdown completes
* *AND* the library SHALL respect the configured retry timeout while flushing

### Scenario: Stops background threads after close

* *GIVEN* the telemetry client has been closed
* *WHEN* all eligible queued work has completed or timed out
* *THEN* the library SHALL stop its background sender threads
* *AND* the library MUST NOT continue background telemetry work after close returns
