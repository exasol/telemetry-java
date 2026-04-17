# Feature: client-identity

Defines the required product identity values that the host application configures once and the library attaches to every emitted telemetry message.

## Requirement: Client Identity
`req~client-identity~1`

The library shall require caller-configured identity values and attach them to emitted telemetry messages as described by the scenarios below.

Covers:
* `feat~client-identity~1`

Needs: impl, utest, itest

## Background

Telemetry messages carry three stable identity fields: `category`, which is the configured project tag; `version`, which is the telemetry protocol version; and `productVersion`, which is the integrating product or library version. Feature names remain arbitrary caller-provided strings and MUST NOT duplicate project identity.

## Scenarios

### Scenario: Requires project tag and productVersion when creating telemetry configuration

* *GIVEN* the host application creates telemetry configuration
* *WHEN* the host application provides a blank project tag or a blank `productVersion`
* *THEN* the library SHALL reject configuration creation
* *AND* the library MUST require both values before a telemetry client can be created

### Scenario: Attaches configured identity values to emitted telemetry messages

* *GIVEN* the library is configured with a project tag and `productVersion`
* *WHEN* the library emits a telemetry message
* *THEN* the library SHALL emit the configured project tag as `category`
* *AND* the library SHALL emit the configured `productVersion` as `productVersion`
* *AND* the library SHALL keep `version` reserved for telemetry protocol version `0.2.0`
