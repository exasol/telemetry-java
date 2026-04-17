# AGENTS.md

## Project Conventions

- The project uses the `speq` skill, see https://github.com/marconae/speq-skill
- The project uses OpenFastTrace (OFT).
  - Reference skill: https://github.com/itsallcode/openfasttrace/blob/main/skills/openfasttrace-skill/SKILL.md
- The project uses [project-keeper](https://github.com/exasol/project-keeper) for managing project structure

## OpenFastTrace

### OpenFastTrace Artifact Types

- `feat`: high level features in the mission
- `req`: requirements in the speq spec files
- `impl`: implementation in code
- `utest`: unit tests
- `itest`: integration tests

### Tag Maintenance

- Always add or update OpenFastTrace tags for features, requirements, and code tags when updating the mission or speq spec files.

### Validation

- Ensure that all requirements are covered by running `mvn generate-sources org.itsallcode:openfasttrace-maven-plugin:trace`.

## Project Keeper

If `mvn verify` fails with project keeper errors, run `mvn project-keeper:fix` and try again.
