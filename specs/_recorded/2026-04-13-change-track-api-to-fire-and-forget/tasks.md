# Tasks: change-track-api-to-fire-and-forget

- [x] Remove the `track(feature, attributes)` overload from `TelemetryClient`.
- [x] Change `track(feature)` to return `void` and make disabled, invalid, closed, and queue-full calls silent no-ops.
- [x] Remove the `TrackingResult` API and migrate unit and integration tests to assert side effects instead of return values.
- [x] Update developer-facing documentation for the simplified tracking API.
