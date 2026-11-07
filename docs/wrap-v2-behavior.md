# WRAP of the format V2: behavior by release

[Русская версия](wrap-v2-behavior_ru.md)

This document describes what WRAP of Oracle 10g and later writes, release by release, as far as it matters for restoring
the source. The library follows it as [ADR 0007](adr/0007-line-endings-of-restored-text.md) and
[ADR 0009](adr/0009-format-v2.md) decide.

The distributions of 10.1.0.3, 10.2.0.1, 11.1.0.6, 11.2.0.1, 12.1.0.2, 12.2.0.1, 18.3, 19.3, 21.3 and 23.26.1 have no
WRAP program and no source of it: WRAP is linked from their object files at installation. The rules below were read in
these object files and checked with WRAP for Linux of the same releases and of 10.2.0.5, which is available only as a
program, on synthetic sources and on the wrapped files of the distributions. A rule marked *by the code* was not checked
with a program.

## Releases

| Release | Implementation |
|---|---|
| 10.1.0.3 | A scanner of its own that reads the source line by line. It checks the syntax: a unit with an error stays plain text, WRAP prints the error and exits with code 0. |
| 10.2.0.1 to 23.26.1 | Works on the lexemes of the PL/SQL scanner and does not check the syntax; only text it cannot split into lexemes stays plain, with `PL/SQL Wrapper error at line N`. The database wraps with the same code (`DBMS_DDL`): the whole input is one unit, no `/` line is looked for or written (*by the code*). |

WRAP 12.1.0.2, 12.2.0.1, 18.3, 19.3 and 21.3 give the same files from the same sources with LF, except for units with
`EDITIONABLE` or `NONEDITIONABLE`, which only 21.3 wraps. WRAP 23.26.1 (Oracle AI Database 26ai) gives the same files as
21.3, except for units with `IF NOT EXISTS` and types with `ACCESSIBLE BY`.

## Layout of a unit

All releases from 10.1.0.3 to 23.26.1 write the same layout:

| Line | Content |
|---|---|
| 1 | the header (see below), then ` wrapped `; it may span several lines |
| 2 | `a000000` |
| 3 | hexadecimal ID of the character set of `NLS_LANG`: `1` US7ASCII, `1f` WE8ISO8859P1, `ab` CL8MSWIN1251, `369` AL32UTF8 and so on; `1` without `NLS_LANG` |
| 4–18 | `abcd` |
| 19 | unit kind, the object type of the data dictionary: `7` procedure, `8` function, `9` package, `b` package body, `d` type, `e` type body, `16` library, `57` assembly (11.1.0.6 and later) |
| 20 | the length of the text with its terminating NUL, and the number of Base64 characters plus one per full line of 72 |
| 21... | Base64 lines of 72 characters, an empty line and a `/` line |

The data is the SHA-1 digest of a zlib stream followed by the stream, which holds the text and a NUL. Every byte `x`
of it is written as `F[(x + 0xA0) mod 256]`, and is read back as `R[b] − 0xA0`, where `F` and `R` are two inverse
permutations of 256 bytes. They are the same in all releases; the table of `V2Data` is `R` with `0xA0` subtracted.

The stream is compressed at level 9. Up to 11.1.0.6 WRAP uses a zlib of its own, from 11.2.0.1 on a compression layer
of the Oracle libraries, and the same text may come out as a different stream: WRAP 11.2.0.1 compresses 2 of 169 test
scripts differently from WRAP 10.2.0.5. Releases 11.2.0.1 to 23.26.1 give the same stream.

## How the database reads a unit

The loader of 10.2.0.1 to 23.26.1 (*by the code*):

- takes the number on line 2 as a version word `v`, requires its first byte to be `0x0A` and subtracts
  `0xA0 | ((v >> 20) & 0xF)` instead of `0xA0`: with `a000000` the two are the same. The loader of 18.3 and later also
  has a rule for a first byte from `0x12` on, which the check before it makes unreachable;
- reads the numbers of lines 3 to 20 separated by LF and spaces only, so a CR there is an error, and does not check the
  `abcd` lines and the kind;
- reads exactly as many characters after the lengths as the second length gives, skipping all but the Base64 alphabet,
  checks the digest and converts the text from the character set of line 3 to the one of the database;
- in 11.2.0.1 and later can refuse a unit of the format V1 with `PLS-01918`. Oracle documents the parameter
  `PERMIT_92_WRAP_FORMAT` for it: `TRUE` by default in 19c, `FALSE` in 23ai and later.

## What is wrapped

| Source | 10.1.0.3 | 10.2.0.1 to 11.2.0.1 | 12.1.0.2 to 19.3 | 21.3 | 23.26.1 |
|---|---|---|---|---|---|
| `CREATE [OR REPLACE] PROCEDURE`, `FUNCTION`, `PACKAGE [BODY]`, `TYPE [BODY]`, `LIBRARY` | wrapped | wrapped | wrapped | wrapped | wrapped |
| `CREATE [OR REPLACE] ASSEMBLY` | plain text | wrapped from 11.1.0.6 | wrapped | wrapped | wrapped |
| `CREATE [OR REPLACE] EDITIONABLE` or `NONEDITIONABLE` unit | plain text | plain text | plain text | wrapped | wrapped |
| `CREATE kind IF NOT EXISTS name` | plain text | `IF` taken for the name | `IF` taken for the name | `IF` taken for the name | wrapped |
| type with `ACCESSIBLE BY (…)` before `AS OBJECT` or `UNDER` | plain text | plain text | plain text | plain text | wrapped |
| trigger, anonymous block | plain text | plain text | plain text | plain text | plain text |

- A type is wrapped only if the header clauses, if any, are followed by `AUTHID DEFINER` or `AUTHID CURRENT_USER`, if
  any, and then by `UNDER` or `AS OBJECT` (`IS OBJECT`). WRAP 23.26.1 also accepts `ACCESSIBLE BY (…)` before or after
  `AUTHID`; it stays in the text. A collection type and an incomplete type stay plain text in all releases.
- WRAP 23.26.1 accepts `IF NOT EXISTS` after the kind and `BODY` of every kind of unit, with `EDITIONABLE` too, but not
  with `OR REPLACE`: then it prints `PL/SQL Wrapper error at line 0` and leaves the unit plain text. WRAP 10.2.0.1 to
  21.3 take `IF` for the name of the unit and leave the rest in the text, so a type with `IF NOT EXISTS` stays plain
  text; WRAP 10.1.0.3 leaves the unit plain text as a syntax error.
- A unit that already has `wrapped` after its header is copied unchanged. WRAP 10.2.0.5, 11.2.0.1 and later do the same
  with `COMPILED` and `COMPRESS` in that place, while WRAP 10.2.0.1 and 11.1.0.6 wrap such a unit and WRAP 10.1.0.3
  leaves it plain text as a syntax error.

## The header

The header is the `CREATE` statement of the source up to the end of the name or of the last clause below, copied as
written, with its comments and line breaks. The clauses go into the header and out of the text of the unit, and so
does `IF NOT EXISTS` between the kind and the name in WRAP 23.26.1.

| After the name | 10.1.0.3 | 10.2.0.1 to 11.2.0.1 | 12.1.0.2 to 23.26.1 |
|---|---|---|---|
| `SHARING = METADATA`, `OBJECT` or `NONE` | syntax error, plain text | stays in the text | header, every kind |
| `SHARING = DATA` or `EXTENDED DATA` | syntax error, plain text | stays in the text | the unit stays plain text |
| `TIMESTAMP 'ts'` or `TIMESTAMP OLD` | header, every kind | header, every kind | header, every kind |
| `OID 'oid'` | header, every kind | header for a type, in the text for other kinds | header for a type, in the text for other kinds |

The clauses come in the order `SHARING`, `TIMESTAMP`, `OID`. A type with `OID` before `TIMESTAMP` or with `SHARING`
after `OID` stays plain text. A `SHARING` clause in another place, after `AUTHID`, `ACCESSIBLE BY` or the return type
of a function, stays in the text.

WRAP 10.1.0.3 writes the first blank after the last clause and then ` wrapped `, so the header ends with two spaces, or
with a line break and a space, before `wrapped`; a type body gets two spaces without clauses too. A unit other than a
type gets NUL there if the last clause ends its line: WRAP 10.1.0.3 reads the source line by line and finds the end of
the line, while for a type it has read the next line already. In the text of the unit WRAP 10.1.0.3 replaces the
clauses with spaces, and WRAP 10.2.0.1 and later leave them out.

WRAP 10.2.0.1 and later copy the blanks before the header, unless the unit follows the `/` of another unit or of an
anonymous block. WRAP 10.1.0.3 drops them.

## The text of the unit

- The text starts with the kind in upper case and the name, without `CREATE`, `OR REPLACE`, `EDITIONABLE`,
  `IF NOT EXISTS` and the schema:
  - WRAP 10.1.0.3 keeps the blanks between the kind and the name, writes spaces in place of the schema and writes the
    name in upper case;
  - WRAP 10.2.0.1 separates them with one space and writes the name in upper case, a quoted name too;
  - WRAP 10.2.0.5 to 23.26.1 separate them with one space and keep the name as in the source.
- Unquoted words are in upper case by the rules of the character set of `NLS_LANG`. String literals, quoted identifiers
  and hints keep their case.
- Comments are removed, their line breaks stay. A comment between two words is removed without a separator:
  `tz_obj/* c */as` becomes `tz_objAS`.
- The end of the text:
  - WRAP 10.2.0.1 to 23.26.1 end it with the last lexeme of the unit, unless a comment follows it: then the text goes up
    to the comment, with the blanks and line breaks before it;
  - WRAP 10.1.0.3 ends it with the last lexeme only if the line ends right after it and the next line is the `/` line,
    and otherwise keeps the text up to the `/`;
  - all releases remove a block comment after the last lexeme together with the next line break.
- A unit without a `/` line takes the text up to the next `/` line or up to the end of the file.

## Line endings

All releases write LF and accept a source with CRLF.

| Release | CR of the source |
|---|---|
| 10.1.0.3 | Every CR right before LF becomes a space, in string literals too. A CR elsewhere outside a literal is a syntax error, and the unit stays plain text. |
| 10.2.0.1 to 11.2.0.1 | A CR right before LF becomes a space, in string literals too, but only within each block of 512 bytes that WRAP reads from the start of the file: a CR in the last byte of a block, at the offset 511, 1023 and so on, stays as CRLF. A CR before a removed comment stays too: `\r-- c\n` gives `\r\n`. |
| 12.1.0.2 to 23.26.1 | Every CR stays, in string literals too. |

A source that ends with `/` without a line break gives a wrapped file that ends the same way with WRAP 10.2.0.1 to
23.26.1; WRAP 10.1.0.3 always writes a line break after the last `/`.

## Text outside the units

WRAP copies the text outside the units, except for these lines, which all releases from 10.1.0.3 to 23.26.1 remove
between the statements and inside an SQL statement over several lines:

- empty lines and lines of blanks;
- `REM` lines: the word `REM` in any case, alone or followed by a blank, also after blanks. `REMARK` lines and other
  words that start with `REM` stay;
- `--` comment lines, also after blanks.

Inside an anonymous block, up to its `/` line, WRAP 10.2.0.1 and later keep most of them (see the table). Such a line
between the statements of a wrapped file was added after wrapping.

| Outside the units | 10.1.0.3 | 10.2.0.1 to 11.2.0.1 | 12.1.0.2 to 23.26.1 |
|---|---|---|---|
| an empty line, a line of blanks or a `REM` line inside an anonymous block | removed | stays | stays |
| a `--` line inside an anonymous block | removed | an empty line in its place | an empty line in its place |
| a block comment at the start of a line | stays | stays | removed, the rest of the line stays |
| a block comment after a statement on its line | stays | stays | stays |
| blanks at the start of a line after the `/` of a unit or of an anonymous block | stay before plain SQL, dropped before a header | dropped | dropped |
| blanks at the start of a line after the `/` of an SQL statement | stay | stay | stay |

A `--` line right after a `/` line is removed by every release, and the blanks of the next line then stay.
