# Oracle PL/SQL Unwrapper — Presentation Outline

## Slide 1 — Oracle PL/SQL Unwrapper
- Recover readable PL/SQL from supported Oracle wrap V2 objects
- Free open-source Java library and command-line interface
- Clear route for V1 and complex cases

**Speaker notes:** Introduce the project and its scope. The public project restores V2; it recognizes V1 but does not unwrap it. Keep the distinction clear from the beginning.

## Slide 2 — When source is missing
- Closed projects and missing repositories
- Contractors or maintainers no longer available
- Legacy maintenance and audit needs
- Migration tools cannot read wrapped code as ordinary source

**Speaker notes:** These are examples of the problem space, not claims about particular customers or case outcomes. The database object may remain even when the source workflow does not.

## Slide 3 — Two formats, different paths
- V1: Oracle 7.2–9.2
- V2: Oracle 10g through 26ai+
- Public project supports V2 and recognizes V1
- V1 is not unwrapped by the public project

**Speaker notes:** A single “unwrap” button should not imply that all formats have the same support. V2 and V1 require different routes.

## Slide 4 — Free V2 capability
- Java 8+ library and CLI
- Maven Central: com.github.sftwnd:oracle-plsql-unwrapper:1.0.0
- Native releases: Linux x64, Windows x64, macOS arm64/x64
- BSD-3-Clause license

**Speaker notes:** Direct developers to the repository and release page for usage details. V2 does not record the original PL/SQL version.

## Slide 5 — A focused web workflow
- Paste one wrapped object or upload one .sql/.plb file
- Confirm lawful rights before submitting
- V2 returns readable source and a short status
- No source execution; no public bulk UI

**Speaker notes:** A single script with multiple wrapped objects is directed to the CLI. The tool processes a request in memory, does not write submitted source to disk or log its contents, and is designed not to retain the request after the response. Do not claim physical memory zeroization.

## Slide 6 — Oracle 23ai/26ai and PLS-01918
- By default, code wrapped with wrap 9.2 or older reports PLS-01918 on Oracle 23ai/26ai
- This is not a universal claim for every database or configuration
- If source is missing, identify the wrap format before choosing a recovery path

**Speaker notes:** Avoid stating that every Oracle 23ai/26ai installation rejects every older wrapped object. Use the qualifying phrase “by default.”

## Slide 7 — What recovery returns
- Program logic and identifiers may be recovered
- Comments and original formatting are usually lost
- V2 output reflects the representation retained by WRAP
- Review and validate the result in its target environment

**Speaker notes:** Do not promise the original source file or exact formatting. The V2 format does not store the PL/SQL version.

## Slide 8 — When to contact the author
- V1 and difficult cases
- Managed bulk recovery
- PLS-01918 / Oracle 23ai/26ai questions
- Re-wrap for a specified target version, not below the original

**Speaker notes:** The commercial service covers wrapped PL/SQL from 7.2 through 26ai+, subject to eligibility. It is a separate service from the public V2 project.

## Slide 9 — Rights and next steps
- Demonstrate lawful rights to the code
- Written confirmation and NDA before commercial work
- Do not submit Oracle internal packages or unauthorized third-party code
- Start with V2, use the CLI for local multi-object scripts, or contact the author for eligible complex work

**Speaker notes:** The web checkbox is an attestation, not proof of rights or legal advice. Keep the next step practical and non-aggressive.

## Slide 10 — Links
- Repository: https://github.com/sftwnd/oracle-plsql-unwrapper
- Releases: https://github.com/sftwnd/oracle-plsql-unwrapper/releases
- Maven Central: https://central.sonatype.com/artifact/com.github.sftwnd/oracle-plsql-unwrapper
- Author: ashindarev@gmail.com · https://www.linkedin.com/in/ashindarev/

**Speaker notes:** Close with both paths: free V2 support and direct contact for V1 or complex work. Reiterate that commercial work requires demonstrated rights, written confirmation, and an NDA.
