## Oracle PL/SQL Unwrapper

[Oracle PL/SQL Unwrapper](https://github.com/sftwnd/oracle-plsql-unwrapper) is a free Java 8+ library and CLI for restoring PL/SQL source from Oracle wrap format V2 (Oracle 10g through 26ai+). The artifact `com.github.sftwnd:oracle-plsql-unwrapper:1.0.0` is available on Maven Central under the BSD-3-Clause license. [Releases and native binaries](https://github.com/sftwnd/oracle-plsql-unwrapper/releases) are available for Linux x64, Windows x64, and macOS arm64/x64.

The project recognizes Oracle wrap V1 (7.2–9.2) but does not unwrap it. V2 does not record the original PL/SQL version; comments and original formatting are usually lost in recovery. Use the CLI for SQL scripts containing multiple wrapped objects. By default, Oracle 23ai/26ai reports PLS-01918 for code wrapped with wrap 9.2 or older; behavior is not universal.

For V1, bulk, PLS-01918, or complex recovery, contact [Andrey Shindarev by email](mailto:ashindarev@gmail.com) or [LinkedIn](https://www.linkedin.com/in/ashindarev/). Commercial work requires demonstrated rights to the code, written confirmation, and an NDA. Re-wrapping can target a specified Oracle version, but not one older than the original code.
