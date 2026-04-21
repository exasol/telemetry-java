# Integration Guide

`telemetry-java` is a Java 11 library with zero runtime dependencies.

## Example Integration

```java
String projectShortTag = "MyApp";
TelemetryConfig config = TelemetryConfig.builder(projectShortTag, "1.2.3").build();

try (TelemetryClient client = TelemetryClient.create(config)) {
    client.track("checkout-started");
}
```

## Required Configuration

- A project short tag and a product/library version at startup. The library adds the project tag as the telemetry category and includes the configured productVersion in every accepted telemetry event.
- An optional HTTP endpoint for JSON `POST` delivery. If omitted, the default endpoint is `https://metrics.exasol.com`.
- An optional host-controlled disable switch via `TelemetryConfig.Builder.disableTracking()` when the integrating application exposes its own telemetry setting.

## Required Documentation

Applications integrating `telemetry-java` need to link to the [App User Guide](app-user-guide.md) in both:

- their changelog entry for introducing telemetry
- their end-user documentation

Example changelog entry:

```markdown
## Summary

This release adds anonymous feature-usage telemetry via `telemetry-java`. See the [documentation](https://github.com/exasol/telemetry-java/blob/main/doc/app-user-guide.md) for details on collected data and opt-out behavior.

## Features

* #??: Added anonymous feature tracking
```

Example end-user documentation entry:

```markdown
## Telemetry

This application uses `telemetry-java` to send anonymous feature-usage events.

For details on what is collected and how to disable telemetry, see the [documentation](https://github.com/exasol/telemetry-java/blob/main/doc/app-user-guide.md).
```

## Environment Variables

- `EXASOL_TELEMETRY_DISABLE`
  Disables collection and delivery when set to any non-empty value.
- `EXASOL_TELEMETRY_ENDPOINT`
  Overrides the endpoint configured in code.
- `CI`
  Disables telemetry automatically when set to any non-empty value.

## Host-Controlled Disablement

If the integrating application already has its own telemetry switch, map that setting to `TelemetryConfig.Builder.disableTracking()`.

```java
TelemetryConfig config = TelemetryConfig.builder("MyApp", "1.2.3")
        .disableTracking()
        .build();
```

When telemetry is disabled this way, the lifecycle `INFO` log reports `Telemetry is disabled via host configuration.`.

### UDF Integration Tests

For Exasol UDF integration tests, disable telemetry explicitly so test executions never emit usage data. In UDF script definitions, set `%env EXASOL_TELEMETRY_DISABLE=1;`.

## Runtime Behavior

- Tracking calls are non-blocking and enqueue events into a bounded in-memory queue.
- Delivery happens on a background sender thread.
- The JSON payload format includes `category`, protocol `version`, `productVersion`, `timestamp`, and `features`.
- Multiple queued events may be batched into a single payload, with timestamps grouped by caller-provided feature name.
- The configured project short tag is emitted as top-level `category`; feature names are preserved as provided, for example `checkout-started`.
- Failed delivery uses exponential backoff and stops when the configured retry timeout is reached.
- Closing `TelemetryClient` flushes pending work before returning and stops background threads.
- Calling `track(...)` after `TelemetryClient` is closed is a no-op.
- The client logs an `INFO` lifecycle message when telemetry is enabled or disabled, and debug-level lifecycle messages when telemetry sends data or stops.
