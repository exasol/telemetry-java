# telemetry-java

`telemetry-java` is a minimal Java library for collecting feature-usage telemetry and sending it to a server over HTTP.

The project is intended for application developers who want a small, auditable telemetry client with zero runtime dependencies. It focuses on feature usage only and deliberately excludes broader SDK behavior such as log collection, stack traces, high-frequency event streams, numeric measurements, or persistent local storage.

## Status

This repository currently contains the MVP implementation and the corresponding recorded specs.

The project uses `speq` skills and recorded specs to drive planning, implementation, verification, and spec recording.

## Features

- Non-blocking telemetry recording through an in-memory queue and background sender
- JSON-over-HTTP `POST` delivery
- Protocol-compatible message format with `version`, `timestamp`, and `features`
- Exponential backoff with retry timeout
- Clean shutdown via `AutoCloseable`
- Environment-variable control for disabling telemetry and overriding the endpoint
- Zero runtime dependencies

## Quick Start

```java
import com.exasol.telemetry.TelemetryClient;
import com.exasol.telemetry.TelemetryConfig;

import java.net.URI;

TelemetryConfig config = TelemetryConfig.builder(
        "my-app",
        URI.create("https://collector.example.com/telemetry"))
        .build();

try (TelemetryClient client = TelemetryClient.create(config)) {
    client.track("startup");
}
```

## Environment Variables

- `EXASOL_TELEMETRY_DISABLE`
  Disables telemetry collection and delivery when set to `true`, `1`, `yes`, or `on`.
- `EXASOL_TELEMETRY_ENDPOINT`
  Overrides the endpoint configured in code.

## Documentation

- [Developer Guide](docs/developer-guide.md)
- [App User Guide](docs/app-user-guide.md)
- [Mission](specs/mission.md)

## Specifications

- [Tracking API](specs/tracking/tracking-api/spec.md)
- [Async Delivery](specs/delivery/async-delivery/spec.md)
- [Shutdown Flush](specs/lifecycle/shutdown-flush/spec.md)
- [Tracking Controls](specs/config/tracking-controls/spec.md)

