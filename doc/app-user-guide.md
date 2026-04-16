# App User Guide

This guide is for end users of applications that use `telemetry-java`.

It explains:

- which data is collected
- how to see whether telemetry is enabled
- how to disable telemetry

## What Is Collected?

The library collects only the data needed for feature-usage telemetry:

- product name, sent as the telemetry category
- product version
- which application features are used
- when those features are used

It does not collect:

- personally identifiable information
- general-purpose diagnostic logs
- stack traces
- high-frequency event streams
- numeric measurements

The library sends telemetry to `https://metrics.exasol.com`.

For Exasol's general privacy information, see the [Exasol Privacy Policy](https://www.exasol.com/privacy-policy/).

## How To See Whether Telemetry Is Enabled

Applications can use lifecycle log messages from the library to show whether telemetry is enabled or disabled.

When telemetry is disabled, the library does not enqueue or send usage events.

## How To Disable Telemetry

Host applications can disable telemetry globally by setting environment variable `EXASOL_TELEMETRY_DISABLE` to any non-empty value.

In Exasol UDFs, set the environment variable in the script definition with `%env`. Example:

```sql
CREATE OR REPLACE JAVA SCALAR SCRIPT MY_UDF(...) RETURNS VARCHAR(100) AS
%env EXASOL_TELEMETRY_DISABLE=1;
/
```

In Exasol UDF script options, each environment variable declaration must end with a semicolon and the value must not be quoted.

Telemetry is also disabled automatically when environment variable `CI` is set to any non-empty value, so CI and test environments do not emit usage data by default.
