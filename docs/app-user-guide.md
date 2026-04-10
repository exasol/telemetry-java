# App User Guide

Applications using `telemetry-java` are expected to inform users when usage tracking is active.

## What Is Collected

The library is designed to send feature-usage events only. It does not collect logs, stack traces, high-frequency data, numeric data, or personally identifiable information.

## Opt-Out

Host applications can disable telemetry globally by setting `EXASOL_TELEMETRY_DISABLE=true`.

When telemetry is disabled, the library does not enqueue or send usage events.
