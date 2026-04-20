# Developer Guide

This documentation is split by audience:

- [Integration Guide](integration-guide.md) for developers who add `telemetry-java` to an application or library.
- This file for developers working on `telemetry-java` itself.

## Build and Test

Standard Maven commands are used:

- `mvn test`
- `mvn verify`
- `mvn package`

## Project Workflow

This project uses `speq-skill` and recorded specs for mission definition, planning, implementation, verification, and recording into the permanent spec library. See [speq-skill documentation](https://github.com/marconae/speq-skill?tab=readme-ov-file) for details.

## OpenFastTrace Requirement Tracing

OpenFastTrace tags are included in the speq-skill spec to avoid duplication.

Tracing runs in the Maven `verify` phase. You can specifically run tracing using this command:

```sh
mvn generate-sources openfasttrace:trace
```
