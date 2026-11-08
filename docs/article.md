# When Wrapped PL/SQL Is All That Remains

A package has been running in a database for years, but the source is no longer where anyone expects it to be. The project may be closed, a repository may be missing, or the contractor who maintained the code may no longer be available. In the database, the object is still there, but its source appears as wrapped text.

This situation is manageable until someone needs to read, change, audit, or move the code. It can become especially urgent during a database upgrade or a migration to another platform.

## Start with the format

Oracle wrap files use two formats relevant to this discussion. The public Oracle PL/SQL Unwrapper project supports V2, used from Oracle 10g onward, through 26ai+. It recognizes V1, used by Oracle 7.2–9.2, but does not unwrap it.

That distinction matters. A V2 file can be handled with the free open-source Java library, command-line interface, or native release. A V1 file needs a different path; the public project reports that it is not implemented and provides the author's contact details. The V1 format is not part of the open-source unwrapping implementation, and this article does not describe or reproduce a V1 method.

The project is available as `com.github.sftwnd:oracle-plsql-unwrapper:1.0.0` on Maven Central under the BSD-3-Clause license. It requires Java 8 or later to run. The project repository and its releases provide the library, command-line distribution, and native binaries.

- [Project repository](https://github.com/sftwnd/oracle-plsql-unwrapper)
- [Releases](https://github.com/sftwnd/oracle-plsql-unwrapper/releases)
- [Maven Central artifact](https://central.sonatype.com/artifact/com.github.sftwnd/oracle-plsql-unwrapper)

## The free V2 route

For a single supported V2 object, the open-source project restores readable PL/SQL. The library can also process SQL scripts containing wrapped units, making the CLI useful for local multi-object work. A lightweight web tool is intended for a single object per request; it does not offer a public bulk-processing interface.

The V2 format does not record the original PL/SQL version. The result is the source representation retained by WRAP, not a reconstruction of every detail of the original file. In particular, comments and original formatting are usually lost. Program logic and identifiers may be recovered, but the output should not be presented as a byte-for-byte restoration of the original source.

The web tool is designed to process submitted code in request memory without writing it to disk or logging its contents. It does not execute recovered code. A user must confirm lawful rights to submit the code and must not submit Oracle internal packages or third-party code without authorization. That checkbox is an attestation; it is not an independent verification of ownership.

## Why older wrapped code can matter during an upgrade

A practical issue described in my article is PLS-01918 on Oracle 23ai/26ai. By default, those database versions report this error for code wrapped with wrap 9.2 or older. “By default” is important: this is not a claim that every installation or configuration behaves identically.

If the original source is available, it can be used to prepare code for the target environment. If it is not available, the age and format of the wrapped object determine what recovery path may be relevant. The free V2 project does not unwrap V1, so it is not the tool for a 7.2–9.2 object.

## Other reasons to look for the source

The same missing-source problem appears in several settings:

- **A project has ended.** The database object remains in use, but the source repository or development environment is gone.
- **A migration is planned.** Conversion tools cannot work from wrapped code as if it were ordinary readable source.
- **An audit or legacy change is due.** Teams may need to review or maintain logic that was not preserved in source control.
- **A database upgrade exposes old assumptions.** An older wrap format may behave differently on a newer Oracle Database release.

These situations do not all have the same answer. A V2 object can be tried with the open-source project. V1, bulk recovery, or a complex environment calls for a direct discussion before work is scoped.

## What professional recovery can and cannot promise

For commercial recovery work, I can handle wrapped PL/SQL across versions 7.2 through 26ai+, including difficult V1 cases and bulk requests. If requested, I can also re-wrap code for a specified Oracle Server version, but not for a version older than the original code. Re-wrapping is not an automatic downgrade path.

The expected recovery is program logic and identifiers. Comments and original formatting are usually not available because they are not preserved in the wrapped representation. A recovered file should therefore be reviewed and validated in the customer's environment before it is treated as a maintained source baseline.

## Rights come first

I accept recovery work only when the customer can demonstrate lawful rights to the code. Before work begins, that basis is recorded in writing with a confirmation of rights and an NDA. I do not accept Oracle internal packages or another party's code without authorization. The rights confirmation is a condition for taking on the work, not legal advice.

If you have a V2 object, start with the free project. If the file is V1, you are addressing PLS-01918 on Oracle 23ai/26ai, the request is for many objects, or the case is otherwise complex, contact me with the Oracle version, approximate object count, and the basis for your rights to the code. Do not send code until we have established that the work is eligible.

- [Email ashindarev@gmail.com](mailto:ashindarev@gmail.com)
- [Andrey Shindarev on LinkedIn](https://www.linkedin.com/in/ashindarev/)

For more background, see [Unwrap Oracle PL/SQL 2.2+](https://www.linkedin.com/pulse/unwrap-oracle-plsql-22-andrey-shindarev-vv2xf).

## A practical next step

First identify whether the object is V1 or V2. For V2, try the free library, CLI, or single-object web tool. For V1 and complex requests, ask about the case before planning recovery. In either path, preserve the distinction between readable recovered logic and the original source file—and establish rights to the code before any commercial work begins.
