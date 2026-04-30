# App User Guide

This guide is for end users of applications that use `telemetry-java`.

It explains:

- what data is collected
- how to tell whether telemetry is enabled
- how to disable telemetry

## What Is Collected?

The library collects only the data required for feature-usage telemetry:

- the product name, sent as the telemetry category
- product version
- which features are used
- when those features are used

It does not collect:

- personally identifiable information
- general-purpose diagnostic logs
- stack traces
- high-frequency event streams
- numeric measurements

The library sends telemetry to `https://metrics.exasol.com/telemetry`.

For Exasol's general privacy information, see the [Exasol Privacy Policy](https://www.exasol.com/privacy-policy/).

## How To See Whether Telemetry Is Enabled

Applications can use lifecycle log messages from the library to indicate whether telemetry is enabled or disabled.

When telemetry is disabled, the library does not queue or send usage events.
Applications may disable telemetry through their own configuration and report that state in logs as `Telemetry is disabled via host configuration.`.

## How To Disable Telemetry

### In Java-Based Virtual Schemas

When you create or update a Java-based virtual schema, disable telemetry by setting the adapter property `TELEMETRY=false`. Example:

```sql
CREATE VIRTUAL SCHEMA hive USING adapter.jdbc_adapter
WITH
  CONNECTION_STRING = 'jdbc:hive2://localhost:10000/default'
  // ...
  TELEMETRY         = 'false';
```

See the [documentation](https://docs.exasol.com/db/latest/sql/create_schema.htm) for details.

### In General Java-Based Exasol UDFs

Set the environment variable `EXASOL_TELEMETRY_DISABLE` to any non-empty value in the script definition with `%env`. Example:

```sql
CREATE OR REPLACE JAVA SCALAR SCRIPT MY_UDF(...) RETURNS VARCHAR(100) AS
%env EXASOL_TELEMETRY_DISABLE=1;
/
```

In Exasol UDF script options, each environment variable declaration must end with a semicolon, and the value must not be quoted. See the [UDF documentation](https://docs.exasol.com/db/latest/database_concepts/udf_scripts/udf_overview.htm#Environmentvariables) for details.

### In Other Applications

Disable telemetry by setting the environment variable `EXASOL_TELEMETRY_DISABLE` to any non-empty value.

### In Continuous Integration

Telemetry is also disabled automatically when the environment variable `CI` is set to any non-empty value. This ensures that CI and test environments do not emit usage data by default.
