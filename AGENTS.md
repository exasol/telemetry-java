# AGENTS.md

## Project Conventions

- The project uses the `speq` skill, see https://github.com/marconae/speq-skill
- The project uses OpenFastTrace (OFT).
  - Reference skill: https://github.com/itsallcode/openfasttrace/blob/main/skills/openfasttrace-skill/SKILL.md

## OpenFastTrace Artifact Types

- `feat`: high level features in the mission, covered by `req`
- `req`: requirements in the speq spec files, covered by `scn`
- `scn`: scenarios in the speq spec files, covered by `impl`, `utest`, `itest`
- `impl`: implementation in code
- `utest`: unit tests
- `itest`: integration tests

## Tag Maintenance

- Always add or update OpenFastTrace tags for features, requirements, scenarios, and code tags when updating the mission or speq spec files.

## Validation

- Ensure that all requirements are covered by running `mvn generate-sources org.itsallcode:openfasttrace-maven-plugin:trace`.
