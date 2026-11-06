# Oracle PL/SQL Unwrapper

[Русская версия](README_ru.md)

A library for restoring PL/SQL source code from the Oracle wrapped format V2 (Oracle 10g and later). The public API is in the `com.github.sftwnd.unwrap` package.

A unit of the format V1 (Oracle 7.2, 7.3, 8, 8i and 9i) is recognized, but unwrapping it is not implemented: `UnwrapException` reports it, with the contacts of the author, who provides the code that unwraps V1: ashindarev@gmail.com or [Andrey Shindarev on LinkedIn](https://www.linkedin.com/in/ashindarev/). A V2 unit records no PL/SQL version.

## Requirements

- JDK 17 or newer.
- Internet access the first time Maven and its plugins are downloaded.

You do not need to install Maven separately: the project includes the Maven Wrapper.

## Build

From the project root, run:

```bash
./mvnw --settings settings.xml clean package
```

On Windows:

```bat
mvnw.cmd --settings settings.xml clean package
```

The build runs the tests: they compare the output byte by byte with the reference files in `src/test/resources/samples`.

After a successful build, the JAR library is located here:

```text
target/oracle-plsql-unwrapper-0.1.0-SNAPSHOT.jar
```

## Usage

The entry point is `com.github.sftwnd.unwrap.Unwrapper`. The format of each wrapped unit is detected from its header; the formats are listed in `WrapFormat` (`V1` and `V2`), and only `V2` is unwrapped.

To unwrap an SQL script, including all wrapped units it contains:

```java
import com.github.sftwnd.unwrap.Unwrapper;

String wrappedScript = ...;
String unwrappedScript = new Unwrapper().unwrapScript(wrappedScript);
```

Each wrapped unit, together with its terminating `/` line, is replaced with the restored text. The rest of the script is copied unchanged, including line endings; after a V2 unit a comment line may be added, see [Format V2](#format-v2). If the input ends with the `/` line of the last unit without a line break, the restored script ends with its `/` without a line break too. The restored text uses the line ending of the wrapped unit (CRLF, LF or CR, the one that ends its header line), so the script keeps its line endings, except for a CRLF stored in the text of a V2 unit, see [Format V2](#format-v2); if the header line has none, the line separator of the operating system is used. Line breaks inside string literals follow it too: WRAP for Windows stores them as LF, and in a unit with CRLF they are written as CRLF again.

Converting the line endings of a wrapped file, for example with `core.autocrlf` in git, converts the restored text in the same way and changes nothing else: WRAP stores a line feed inside a literal as `:n` and a carriage return as a single character, and converters leave both alone. To wrap the restored text again, give WRAP the line endings of its platform. WRAP for Linux replaces a CR before LF with a space (10.1–11.2) or keeps it in the text (12.1 and later), so the text restored from a unit wrapped on Windows, or from a file converted to CRLF, has to be converted to LF first. That conversion is safe: WRAP for Windows stores the line breaks of literals as LF anyway.

The restored unit starts with the header of the wrapped unit. WRAP copies the `CREATE` statement from the source up to the end of the unit name: with or without `OR REPLACE`, with the schema, the case, the quotes and the line breaks. WRAP of the same version gives the restored text the same header, see [Format V2](#format-v2).

To read and write SQL files in their original single-byte encoding:

```java
import com.github.sftwnd.unwrap.Unwrapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

Path input = Path.of("S.SQL");
Path output = Path.of("S.SQ");
String wrappedScript = Files.readString(input, StandardCharsets.ISO_8859_1);
String unwrappedScript = new Unwrapper().unwrapScript(wrappedScript);
Files.writeString(output, unwrappedScript, StandardCharsets.ISO_8859_1);
```

The text of a V2 unit is decoded in the character set the unit names, so a script with V2 units is written in an encoding that covers it, see [Format V2](#format-v2).

Options are passed to the constructor, and `new Unwrapper()` uses `UnwrapOptions.defaults()`. The option `keepStrayCr` keeps a stray CR of a V2 text instead of replacing it with a space, see [Format V2](#format-v2):

```java
import com.github.sftwnd.unwrap.UnwrapOptions;
import com.github.sftwnd.unwrap.Unwrapper;

Unwrapper unwrapper = new Unwrapper(UnwrapOptions.defaults().withKeepStrayCr(true));
```

The default of `keepStrayCr` is `false` unless the system property `com.github.sftwnd.unwrap.keepStrayCr` is `true`: with `-Dcom.github.sftwnd.unwrap.keepStrayCr=true` every `new Unwrapper()` keeps stray CRs, so an application can switch it without a change of its code. `withKeepStrayCr` sets the option whatever the property says.

Other `Unwrapper` operations:

- `unwrap` — unwrap the first wrapped unit;
- `unwrapUnits` — unwrap every wrapped unit and get its format, type and name. A V2 unit has `null` in the PL/SQL version, the PL/SQL compatibility group and `staticSqlAsText`, which only a V1 unit records;
- `supports` — check whether the text contains a wrapped unit of a supported format.

Errors in wrapped content are reported as `UnwrapException`.

To compare the result byte by byte with a reference file:

```bash
cmp S.SQ /path/to/U.SQL
```

If `cmp` prints nothing and exits with code 0, the files are identical.

## Format V2

A V2 unit (Oracle 10g and later) holds the source text as WRAP normalized it, so the restored text is that text, not the original source: unquoted words are in upper case, and comments inside the unit are removed while their line breaks stay. String literals, quoted identifiers and hints keep their case. The restored statement is the header of the wrapped unit followed by the text after the kind and the name of the unit. WRAP copies the TIMESTAMP clause of every kind of unit, with a string literal or `OLD`, and the OID clause of a type (WRAP 10.1: of every kind) into the header and leaves them out of the text (WRAP 10.1 leaves spaces in their place, and the restored statement has the clauses there), and it copies the blanks before the header unless it drops them after a `/` (see below); both are kept. A header without `CREATE`, as in text taken from the data dictionary, gets `CREATE OR REPLACE`; headers with `EDITIONABLE` or `NONEDITIONABLE` are kept as written.

The unit names the character set of its text, the one of `NLS_LANG` when it was wrapped, and the text is decoded in it. A unit wrapped without `NLS_LANG` names US7ASCII whatever its bytes are. Such a text, and a text in a character set the library does not know, is read as UTF-8 if it is valid UTF-8 and as ISO-8859-1 otherwise, which keeps every byte. The restored text may therefore hold characters that the encoding the script was read in cannot hold: write the result in an encoding that covers them, for example the one the units were wrapped in, or UTF-8.

WRAP 12.1 and later keep a `SHARING` clause that follows the unit name in the header and leave it out of the text, and WRAP 23.26.1 does so with `IF NOT EXISTS` between the kind and the name; the restored header has them as written. Units of the kind `ASSEMBLY` (WRAP 11.1 and later) are restored like the other kinds. A unit whose header the library cannot read, for example with a clause of a later release, is reported as `UnwrapException`, so it does not stay wrapped in the restored script. `supports` returns `true` for such a text.

WRAP 12.1 and later for Linux keep the CR of a source with CRLF in the text of the unit, in string literals too, and write the wrapped lines with LF. In a file with LF the text of a unit is restored as it is, with its CRLF, so the value of a literal does not change and these releases give the same unit again.

WRAP 10.2 to 11.2 for Linux replace a CR right before LF with a space, in string literals too, but they read the source in blocks of 512 bytes and do so within a block only: a CR in the last byte of a block, at the offset 511, 1023 and so on of the source file, stays before the LF that starts the next block. A source with CRLF therefore gives a text whose lines end with a blank, with a CRLF on a few of them, at places that depend on the offsets of the source file, comments removed by WRAP included, and not on the program. Such a CR outside string literals and quoted identifiers is stray: by default the restored text has a space in its place, as its other lines have, and a CR inside a literal or a quoted identifier stays as part of its value. A text counts as such if more of its line breaks outside literals have a blank before them than a CR; the CRs that WRAP 12.1 and later keep stand before the line breaks, so their text stays as it is. With `UnwrapOptions.withKeepStrayCr(true)` the text is restored as the unit holds it. Neither text wraps back to the same file as it is: the CRs stand at other offsets in the restored script, and WRAP replaces them there. The text restored with `keepStrayCr` does once a comment before each CR moves it to the last byte of a block again, so such a file is the same up to stray CRs. In the Oracle distributions these are two ORDS files, `ords_migrate` and `upg_oauth`, with 3 and 1 stray CRs, wrapped by WRAP 10.2 or 11.1 from a source with CRLF.

The data of a unit is checked by its SHA-1 digest and its length, and damaged or truncated data is reported as `UnwrapException`. The Base64 lines may be split, joined or have other line endings, as SQL clients and version control leave them.

A removed comment may leave a trace that the next pass of WRAP would lose, so the restored script marks it. A text that ends with a blank or a line break, as it does before a removed comment, gets an empty comment `/**/` after it: every release keeps the text before such a comment and removes the comment with the next line break. After a V2 unit, a line that follows a `/` line and starts with blanks gets a `--` line before it: after the `/` of a unit or of an anonymous block WRAP 10.2 and later keep such blanks only after a comment line, and every release removes that line. The marks cannot be confused with the source: WRAP 10g and later remove every comment from a V2 unit and every `--` line right after a `/` line. So in a script restored from their output, a `/**/` at the end of the text of a V2 unit, before the line break and its `/` line, and a line that holds only `--` right after a `/` line are always marks. Remove them to get the text without the marks; WRAP will then no longer give the same wrapped file from it.

WRAP 10.2.0.5 and 11.1.0.6 give the same wrapped file from the restored text, with one exception: a comment between the unit name and the next word is removed by WRAP without a separator, and the restored text has a space there. Both releases give the same files from the sources checked. WRAP for Linux replaces CR with a space, in string literals too, so the restored text is converted to LF before it is wrapped again.

WRAP 10.1.0.3 gives the same wrapped file too. It keeps more of the source than WRAP 10.2.0.5: the name in the text is in upper case, and the blanks and blank lines before the `/` stay in the text and get the empty comment as well. All these releases write `a000000`, but WRAP 10.1 gives a different text from the same source, so a unit is wrapped again by the release that wrapped it.

WRAP removes `REM` lines, `--` comment lines and empty lines between the statements outside the units, so such a line stands in a wrapped file only if it was added after wrapping, as the `Rem drv:` lines of some Enterprise Manager files are, or the block of `Rem` lines and the empty lines around the statements of the `prvtqopi` and `prvtsqlpatch` files. The restored script keeps it, as all text outside the units, and wrapping the script again gives the file without it. Inside an anonymous block WRAP 10.2 and later keep empty lines and `REM` lines.

## License

The library is distributed under the [GNU Affero General Public License, version 3](LICENSE) (AGPL-3.0-only). Anyone who
uses, modifies or distributes it, or lets users interact with it over a network, must make the source code of the
resulting work available under the same license.

A commercial license without the AGPL obligations, for use in closed-source products and services, is available from the
copyright holder under a separate agreement: write to ashindarev@gmail.com or contact
[Andrey Shindarev on LinkedIn](https://www.linkedin.com/in/ashindarev/).

## Contributing

The project does not accept third-party contributions: pull requests and patches from outside are closed without review.
Bug reports and questions are welcome as issues, but the code is written by the copyright holder only. This keeps all
rights to the code with one holder and makes the commercial license possible. The AGPL-3.0 gives you the right to fork
the project and develop the fork under the same license.
