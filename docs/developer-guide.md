# Developer Guide

## Integration

`telemetry-java` is a Java 11 library with zero runtime dependencies.

Example integration:

```java
TelemetryConfig config = TelemetryConfig.builder("shop-ui", URI.create("https://collector.example/telemetry"))
        .build();

try (TelemetryClient client = TelemetryClient.create(config)) {
    client.track("checkout-started", Map.of("screen", "basket"));
}
```

## Required Configuration

- A project short tag at startup. The library adds it to every accepted telemetry event.
- A configured HTTP endpoint for JSON `POST` delivery.

## Environment Variables

- `EXASOL_TELEMETRY_DISABLE`
  Disables collection and delivery when set to `true`, `1`, `yes`, or `on`.
- `EXASOL_TELEMETRY_ENDPOINT`
  Overrides the endpoint configured in code.

## Runtime Behavior

- Tracking calls are non-blocking and enqueue events into a bounded in-memory queue.
- Delivery happens on a background sender thread.
- Failed delivery uses exponential backoff and stops when the configured retry timeout is reached.
- Closing `TelemetryClient` flushes pending work before returning and stops background threads.
