Suggested commands for telemetry-java:

- Build: `mvn package`
- Test: `mvn test`
- Verify: `mvn verify`
- Coverage: `mvn verify` (with JaCoCo configured in the build)
- Static analysis / Sonar: standard Maven Sonar invocation when configured, typically `mvn sonar:sonar`

Notes:
- The project uses standard Maven commands.
- Java target version is 11.