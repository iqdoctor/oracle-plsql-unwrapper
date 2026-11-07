# 0014. Continuous integration and quality analysis

[Русская версия](0014-continuous-integration_ru.md)

## Context

The project promises to work on Java 8 and on later JDKs, as modules and as a class path. A build on one JDK of one
machine does not check that promise, and the state of the project should be visible at the top of its README.

## Decision

- `.github/workflows/build.yml` runs on each push to `master` and `develop` and on each pull request:
  - the job `build` runs `verify` on JDK 8, 11, 17, 21 and 25;
  - the job `sonar` builds on JDK 21 and analyzes the project in SonarCloud: organization `sftwnd-github`, project
    `sftwnd_oracle_plsql_unwrapper`. Without the secret `SONAR_TOKEN` it skips the analysis and passes.
- The Maven profile `coverage` turns on JaCoCo. Each module records its tests, and the module `oracle-plsql-unwrapper`
  builds one report of all the modules in `target/site/jacoco-aggregate`, which the analysis takes.
- The README starts with the badges of Java 8, JPMS, the quality gate, the coverage, the build and the license.

## Rationale

- The launch tests of `verify` already start the unwrapper on the class path and on the module path, so the matrix of JDKs
  checks both without a job of its own.
- The classes of a module are also tested from the modules that use it: the SPI has almost no tests of its own, so a report
  per module would understate it.
- JaCoCo in a profile leaves an ordinary build as it is, on every JDK, and the project `oracle-plsql-unwrapper-pro`, which
  inherits the parent, does not get it.
- A job that passes without the token keeps the build badge honest until SonarCloud is set up.

## Alternatives

- **JaCoCo in every build.** A slower build and one more agent on each JDK, for a report that only the analysis reads.
- **A report per module.** See Rationale.
- **The badges of Maven Central and javadoc.** The artifacts are not published, so they would be broken.
- **A job that starts the jars on the module path.** It repeats the launch tests.

## Consequences

- The analysis starts once the project exists in SonarCloud and the repository has the secret `SONAR_TOKEN`; until then the
  badges of the quality gate and the coverage show nothing.
- The launch tests run in JVMs of their own, so the coverage does not count them.
- The project `oracle-plsql-unwrapper-pro` is private and has no such workflow or badges.
