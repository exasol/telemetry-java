When completing work in telemetry-java:

- Run relevant Maven verification commands using the standard Maven lifecycle.
- Preserve zero-dependency and auditability goals.
- Check shutdown behavior and background-thread lifecycle where applicable.
- Check privacy constraints: no PII, opt-out behavior, and only intended telemetry fields.
- Keep memory usage bounded and avoid persistent local storage unless requirements change.