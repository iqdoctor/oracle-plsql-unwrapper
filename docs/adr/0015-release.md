# 0015. Release

[Русская версия](0015-release_ru.md)

## Context

The library goes to Maven Central, where a published version can never be removed or replaced, and its executables go to
users who may have no Java. A release must therefore be what was tested, be signed, and be checked once more before it is
public. The project `oracle-plsql-unwrapper-pro` inherits the parent of this project and must never reach Maven Central.

## Decision

- A release has an issue `Release X`. A pull request of its branch sets the version `X` in `develop`; `develop` goes to
  `master` by a pull request, and the merge commit in `master` gets the annotated tag `vX`. Another pull request moves
  `develop` to the next `SNAPSHOT`.
- The upload runs from a clean checkout of the tag: `./mvnw -Prelease clean deploy` on JDK 9 or later, with the signing
  key unlocked in gpg-agent. The profile `release`:
  - stops the build unless the version is a release, the JDK is 9 or later and `central.skip` is `false`;
  - signs the artifacts with maven-gpg-plugin;
  - uploads them with central-publishing-maven-plugin. The deployment waits in the Central Publisher Portal as validated,
    and it is published there by hand.
- Maven Central gets the parent POM and the jars of `oracle-plsql-unwrap-spi`, `oracle-plsql-unwrap-v2` and
  `oracle-plsql-unwrapper`, each with its sources, javadoc and tests, and the executable jar of the unwrapper.
- The javadoc is built by every build, with doclint and `failOnWarnings`; it does not ask for a tag of each parameter and
  result, since the comments are sentences.
- The GitHub release `vX` holds the jars, the distribution zip and the native executables: those for Linux x86_64,
  Windows x86_64 and macOS arm64 from the workflow `native` of `master`, and the one for macOS x86_64 built locally with the
  profile `native`. Each native executable is in a zip with `LICENSE`.
- `oracle-plsql-unwrapper-pro` sets `central.skip=true`: the profile `release` stops there.

## Rationale

- The tag on the merge commit of `master` names exactly what was released, and the upload from it repeats the tested build.
- A validated deployment can still be dropped: the check by hand before publishing costs a minute and protects a version
  that cannot be taken back.
- central-publishing-maven-plugin uploads a `SNAPSHOT` even with `skipPublishing`, to the snapshot repository of the Portal,
  so the profile refuses a `SNAPSHOT` instead of relying on that parameter.
- GitHub has no runner of macOS x86_64, so that executable is built on such a machine.
- The versions move by pull requests, as every other change does, and the history keeps the messages of the project.

## Alternatives

- **maven-release-plugin.** It commits and tags by itself, with its own messages, and pushes before the pull requests.
- **autoPublish.** A mistake noticed after the upload would stay on Maven Central forever.
- **Upload from GitHub Actions.** The signing key and the token of the Portal would be secrets of the repository.
- **No javadoc check.** A broken reference would reach javadoc.io unnoticed.

## Consequences

- A release needs the owner of the key: it unlocks the key before the upload and publishes the deployment afterwards.
- The badges of Maven Central and javadoc show the version only once the deployment is published.
- `deploy` without the profile `release` uploads nothing, since the project has no other repository.
