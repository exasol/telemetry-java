# App User Guide

Applications using `telemetry-java` are expected to inform users when usage tracking is active.

## What Is Collected?

The library is designed to send feature-usage events only. It does not collect logs, stack traces, high-frequency data, numeric data, or personally identifiable information.

Messages sent to the server contain the protocol version, the message timestamp, and a `features` map from feature name to a list of usage timestamps.

The library sends telemetry to `https://metrics.exasol.com`.

## Opt-Out

Host applications can disable telemetry globally by setting environment variable `EXASOL_TELEMETRY_DISABLE` to any non-empty value.

Telemetry is also disabled automatically when environment variable `CI` is set to any non-empty value, so CI and test environments do not emit usage data by default.

When telemetry is disabled, the library does not enqueue or send usage events.

The library also emits lifecycle log messages so users can see whether telemetry is enabled or disabled, when data is sent, and when telemetry stops.
