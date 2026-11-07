# 0010. License: BSD-3-Clause, closed to contributions

[Русская версия](0010-license_ru.md)

## Context

This project holds the contract of a wrapped format, the format V2 and the unwrapper. A format is a separate module that
implements the contract, and such a module may be closed: the format V1 is one. An application embeds the unwrapper and
its formats in a product of its own. The code is written by one author, who holds all rights to it.

## Decision

- The license is the BSD 3-Clause License (`BSD-3-Clause`), with the full text in `LICENSE`; the parent `pom.xml` names it
  in `<licenses>`.
- Each jar holds the text of the license of its project in `META-INF/LICENSE`, and a distribution has it at its root.
- The project does not accept third-party contributions. The README says so, and pull requests and patches from outside
  are closed without review. Issues with bug reports and questions stay open.
- Every `.java` file starts with the line `// SPDX-License-Identifier: BSD-3-Clause`, and `pom.xml` names the same
  identifier, so the license can be read from a file taken apart from the repository and by license scanners.

## Rationale

- A closed module can implement the SPI and run with the unwrapper only if the license of the SPI and of the unwrapper
  does not extend to it. A copyleft license would do that.
- A permissive license puts no conditions on the product that embeds the library, so it needs no second license.
- The third clause keeps the name of the copyright holder from promoting derived products.
- Closing contributions keeps the rights with one holder, without a contributor agreement that needs a process and the
  signatures of everyone who contributes.
- A fork stays allowed: the license gives that right to everyone.

## Alternatives

- **AGPL-3.0 with a commercial license.** The code of a product built on the library would have to be opened, or the
  product would need a separate agreement, and a closed format would not fit the SPI.
- **MIT.** Permissive as well, but it has no clause about the name of the copyright holder.
- **Apache-2.0.** Permissive as well, with a patent grant and more conditions on changed files; the project has no use
  for them.

## Consequences

- A product that uses the library, with changes or without them, may stay closed, as long as it keeps the copyright
  notice and the text of the license.
- The license of every dependency must allow that. The library has no third-party dependencies besides the test ones.
- Taking code from a third party into the repository requires a license that allows its use under this one.
- The closed module of the format V1 and the project `oracle-plsql-unwrapper-pro` have terms of their own, and this license
  does not cover them.
