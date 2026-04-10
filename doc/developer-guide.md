# Developer Guide

## Integration

`telemetry-java` is a Java 11 library with zero runtime dependencies.

Example integration:

```java
TelemetryConfig config = TelemetryConfig.builder("shop-ui").build();

try (TelemetryClient client = TelemetryClient.create(config)) {
    client.track("checkout-started");
}
```

## Required Configuration

- A project short tag at startup. The library adds it to every accepted telemetry event.
- An optional HTTP endpoint for JSON `POST` delivery. If omitted, the default endpoint is `https://metrics.exasol.com`.

## Environment Variables

- `EXASOL_TELEMETRY_DISABLE`
  Disables collection and delivery when set to `true`, `1`, `yes`, or `on`.
- `EXASOL_TELEMETRY_ENDPOINT`
  Overrides the endpoint configured in code.
- `CI`
  Disables telemetry automatically when set to `true`, `1`, `yes`, or `on`.

## Runtime Behavior

- Tracking calls are non-blocking and enqueue events into a bounded in-memory queue.
- Delivery happens on a background sender thread.
- The JSON payload format matches the Python protocol shape: `version`, `timestamp`, and `features`.
- The configured project short tag prefixes feature names in the payload, for example `shop-ui.checkout-started`.
- Failed delivery uses exponential backoff and stops when the configured retry timeout is reached.
- Closing `TelemetryClient` flushes pending work before returning and stops background threads.

## Build and Test

Standard Maven commands are used:

- `mvn test`
- `mvn verify`
- `mvn package`

## Project Workflow

This project uses `speq-skill` and recorded specs for mission definition, planning, implementation, verification, and recording into the permanent spec library. See [speq-skill documentation](https://github.com/marconae/speq-skill?tab=readme-ov-file) for details.
