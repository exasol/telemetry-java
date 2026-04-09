Project conventions known so far:

- Keep the implementation minimal and easy to audit.
- Maintain zero external dependencies.
- Favor easy integration for host applications.
- Respect privacy constraints: inform end users, support opt-out, and do not send PII.
- Ensure clean shutdown semantics using AutoCloseable.
- Avoid designs that require persistent local storage or unbounded memory use.