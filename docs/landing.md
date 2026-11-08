# Restore readable PL/SQL from Oracle wrapped V2

## A free, open-source option for V2

Oracle PL/SQL Unwrapper restores readable source from Oracle wrap format V2, used by Oracle 10g and later. Use the public tool for a single V2 object, or use the Java library and command-line interface in your own workflow.

[Open the single-object V2 tool](#try-a-single-object)

[View the project on GitHub](https://github.com/sftwnd/oracle-plsql-unwrapper) · [Browse releases](https://github.com/sftwnd/oracle-plsql-unwrapper/releases) · [Find the Maven artifact](https://central.sonatype.com/artifact/com.github.sftwnd/oracle-plsql-unwrapper)

## What the open-source project supports

- Oracle wrap format V2, from Oracle 10g through 26ai+.
- A Java 8+ library and command-line interface, with native releases for Linux x64, Windows x64, and macOS arm64/x64.
- Maven Central artifact: **com.github.sftwnd:oracle-plsql-unwrapper:1.0.0**.
- BSD-3-Clause license.

V2 records no PL/SQL version. The restored text reflects the form stored by WRAP; it is not a byte-for-byte copy of the original source. Program logic and identifiers can be recovered, while comments and original formatting are usually lost.

## Try a single object

The web tool is intended for one wrapped object per request. Paste the wrapped PL/SQL or select one `.sql` or `.plb` file. The tool identifies V1 or V2, unwraps a supported V2 object, and returns a short status with the result or next step.

Before submitting, confirm that you have lawful rights to the code. Do not submit Oracle internal packages or third-party code unless you are authorized to do so. Submitted source is processed in request memory; it is not written to disk or included in content logs, and the tool does not execute recovered code.

For local scripts containing multiple wrapped objects, use the command-line interface. The web tool does not provide a bulk-processing interface.

## When V1 or a complex case is involved

Oracle wrap format V1 covers Oracle 7.2–9.2. The open-source project can recognize V1, but it does not unwrap it. If V1 is detected, the tool directs you to the author rather than attempting recovery.

Contact Andrey Shindarev for V1, managed bulk recovery, multi-object or otherwise complex requests, or questions about Oracle 23ai/26ai and PLS-01918:

- [Email ashindarev@gmail.com](mailto:ashindarev@gmail.com)
- [Andrey Shindarev on LinkedIn](https://www.linkedin.com/in/ashindarev/)

The author's service covers wrapped PL/SQL from 7.2 through 26ai+, including difficult V1 cases, bulk recovery, and re-wrapping for a specified Oracle version. A re-wrap target cannot be older than the original code version. Work is accepted only when the customer can demonstrate rights to the code; written confirmation and an NDA are required before work begins.

## Oracle 23ai/26ai and PLS-01918

By default, Oracle 23ai/26ai reports PLS-01918 for code wrapped with wrap 9.2 or older. This is not a universal outcome for every database or configuration. If the source is missing, the free V2 tool is not the solution for V1 code; contact the author to discuss whether the case is eligible for recovery.

## Common situations

- The original source is missing after a project closed or a contractor is no longer available.
- A migration away from Oracle is blocked because conversion tools cannot read wrapped code.
- Legacy maintenance or audit work requires readable source.
- An Oracle 23ai/26ai upgrade encounters PLS-01918 for older wrapped code.

The tool restores V2 only. For V1, bulk, or complex work, contact the author directly. Rights to the code must be demonstrated before commercial recovery work can begin.

## Project links

- [Source repository](https://github.com/sftwnd/oracle-plsql-unwrapper)
- [Releases and native binaries](https://github.com/sftwnd/oracle-plsql-unwrapper/releases)
- [Maven Central](https://central.sonatype.com/artifact/com.github.sftwnd/oracle-plsql-unwrapper)
- [BSD-3-Clause license](https://github.com/sftwnd/oracle-plsql-unwrapper/blob/master/LICENSE)
- [Author's reference article](https://www.linkedin.com/pulse/unwrap-oracle-plsql-22-andrey-shindarev-vv2xf)

## Need help with V1 or a complex case?

For eligible V1, bulk, PLS-01918, or other complex recovery work, contact Andrey Shindarev. Please be prepared to demonstrate your rights to the code; written confirmation and an NDA are required before work starts.

[Email ashindarev@gmail.com](mailto:ashindarev@gmail.com) · [LinkedIn](https://www.linkedin.com/in/ashindarev/)
