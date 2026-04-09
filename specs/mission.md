# telemetry-java Mission

## Identity & Purpose

`telemetry-java` is a Java library that can be integrated into other applications to collect and send usage data to a server so developers and app vendors can understand which features are popular.

The project exists because existing solutions collect too much information and include too many features. This library is intended to stay minimal, have zero dependencies, and be easy to audit.

## Target Users

Primary users are developers of other applications who integrate the library into their software.

Application end users are also part of the system context. They must be informed about usage tracking and be able to opt out.

## Typical Workflow

Developers integrate the library into their application with minimal effort.

Application end users use the host application and, when they allow it, the application sends usage data through the library.

## Core Capabilities

- Do not block the main thread while sending usage data.
- Keep integration effort minimal for application developers.
- Provide clean shutdown behavior and ensure queued usage data is sent during shutdown.
- Allow tracking to be deactivated via environment variables.

## Out of Scope

The library does not collect:

- logs
- stack traces
- high-frequency data
- numeric data

## Technical Stack

- Java 11
- Maven
- JUnit Jupiter
- JaCoCo
- Sonar

The project uses standard Maven commands.

## Project Structure

The repository follows the default Maven project structure.

It also includes a `docs/` directory containing:

- an app user guide
- a developer guide

## Architecture

The architecture should be easy to integrate into host applications.

Clean shutdown is handled through `AutoCloseable`.

Intended data flow:

`API call -> queue/buffer -> background sender -> HTTP transport`

## Constraints

- Zero dependencies
- Easily auditable implementation
- End-user opt-out support
- No persistent local storage
- Bounded memory usage
- Retry policy is required
- Only send strings and feature names
- No PII
- No background threads after close
