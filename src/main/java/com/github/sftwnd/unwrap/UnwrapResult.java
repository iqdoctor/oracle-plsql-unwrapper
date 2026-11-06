// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap;

import java.util.Objects;

/**
 * Result of unwrapping one wrapped unit.
 *
 * @param format          format the unit is written in
 * @param plsqlVersion    PL/SQL release that wrapped the unit: exact, for example {@code 2.2.2.3.1}, or a family,
 *                        for example {@code 2.3.x}; {@code null} if the format does not store the version
 * @param plsqlCompatible compatibility group of PL/SQL releases, for example {@code 2.x}; {@code null} if the format
 *                        does not store the version
 * @param staticSqlAsText whether the static SQL is kept as text, as written in the source, rather than parsed into the
 *                        tree; {@code null} if the unit has no static SQL or the format does not keep this
 * @param unitType        unit type from the header, for example {@code PACKAGE BODY}
 * @param unitName        unit name from the header; with the schema if one is given: {@code SCOTT.PKG}
 * @param unwrappedSource restored CREATE statement with the terminating {@code /} line
 */
public record UnwrapResult(
        WrapFormat format,
        String plsqlVersion,
        String plsqlCompatible,
        Boolean staticSqlAsText,
        String unitType,
        String unitName,
        String unwrappedSource) {

    public UnwrapResult {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(unitType, "unitType");
        Objects.requireNonNull(unitName, "unitName");
        Objects.requireNonNull(unwrappedSource, "unwrappedSource");
    }
}
