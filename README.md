# telemetry-java

`telemetry-java` is a minimal Java library for collecting feature-usage telemetry and sending it to a server over HTTP.

[![Build Status](https://github.com/exasol/telemetry-java/actions/workflows/ci-build.yml/badge.svg)](https://github.com/exasol/telemetry-java/actions/workflows/ci-build.yml)
[![Maven Central &ndash; telemetry-java](https://img.shields.io/maven-central/v/com.exasol/telemetry-java)](https://search.maven.org/artifact/com.exasol/telemetry-java)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Atelemetry-java&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.exasol%3Atelemetry-java)

[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Atelemetry-java&metric=security_rating)](https://sonarcloud.io/dashboard?id=com.exasol%3Atelemetry-java)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Atelemetry-java&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=com.exasol%3Atelemetry-java)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Atelemetry-java&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=com.exasol%3Atelemetry-java)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Atelemetry-java&metric=sqale_index)](https://sonarcloud.io/dashboard?id=com.exasol%3Atelemetry-java)

[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Atelemetry-java&metric=code_smells)](https://sonarcloud.io/dashboard?id=com.exasol%3Atelemetry-java)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Atelemetry-java&metric=coverage)](https://sonarcloud.io/dashboard?id=com.exasol%3Atelemetry-java)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Atelemetry-java&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=com.exasol%3Atelemetry-java)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Atelemetry-java&metric=ncloc)](https://sonarcloud.io/dashboard?id=com.exasol%3Atelemetry-java)

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
- Lifecycle log messages for enabled, disabled, sent-count, and stopped telemetry states
- Zero runtime dependencies

## Quick Start

```java
import com.exasol.telemetry.TelemetryClient;
import com.exasol.telemetry.TelemetryConfig;
TelemetryConfig config = TelemetryConfig.builder("my-app").build();

try (TelemetryClient client = TelemetryClient.create(config)) {
    client.track("startup");
}
```

## Environment Variables

* `EXASOL_TELEMETRY_DISABLE`
  Disables telemetry collection and delivery when set to any non-empty value.
* `EXASOL_TELEMETRY_ENDPOINT`
  Overrides the endpoint configured in code.

The default endpoint is `https://metrics.exasol.com`.

## Documentation

* [Developer Guide](doc/developer-guide.md)
* [Integration Guide](doc/integration-guide.md)
* [Library Maintenance Guide](doc/developer-guide.md)
* [App User Guide](doc/app-user-guide.md)
* [Mission](specs/mission.md)
* [Dependencies](dependencies.md)
* [Changelog](doc/changes/changelog.md)

## Specifications

* [Tracking API](specs/tracking/tracking-api/spec.md)
* [Async Delivery](specs/delivery/async-delivery/spec.md)
* [Shutdown Flush](specs/lifecycle/shutdown-flush/spec.md)
* [Tracking Controls](specs/config/tracking-controls/spec.md)
