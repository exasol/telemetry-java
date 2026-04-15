# Feature: client-identity

Defines the required product identity values that the host application configures once and the library attaches to every emitted telemetry message.

## Background

Telemetry messages carry two stable identity fields: `category`, which is the configured project tag, and `version`, which is the integrating product or library version. Feature names remain arbitrary caller-provided strings and MUST NOT duplicate project identity.

## Scenarios

### Scenario: Requires project tag and version when creating telemetry configuration

* *GIVEN* the host application creates telemetry configuration
* *WHEN* the host application provides a blank project tag or a blank product/library version
* *THEN* the library SHALL reject configuration creation
* *AND* the library MUST require both values before a telemetry client can be created
