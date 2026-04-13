Project: telemetry-java

Purpose:
- Java library for integration into other applications to collect and send usage data to a server.
- Helps developers and app vendors understand which features are popular.
- Intended to be minimal, zero-dependency, and easy to audit.

Users:
- Primary users are developers integrating the library.
- End users of host apps must be informed about tracking and can opt out.

Core capabilities:
- Non-blocking sending of usage data.
- Minimal integration effort.
- Clean shutdown with pending data flush.
- Tracking can be deactivated via environment variables.

Out of scope:
- Logs
- Stack traces
- High-frequency data
- Numeric data

Architecture notes:
- Clean shutdown via AutoCloseable.
- Data flow: API call -> queue/buffer -> background sender -> HTTP transport.

Constraints:
- Zero dependencies
- No persistent local storage
- Bounded memory
- Retry policy required
- Only send strings/feature names
- No PII
- No background threads after close