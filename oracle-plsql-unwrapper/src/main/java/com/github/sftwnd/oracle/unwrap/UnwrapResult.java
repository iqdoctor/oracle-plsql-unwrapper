// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap;

import java.util.Objects;

/** Result of unwrapping one wrapped unit. The object is immutable. */
public final class UnwrapResult {

    private final String format;
    private final String plsqlVersion;
    private final String plsqlCompatible;
    private final Boolean staticSqlAsText;
    private final String unitType;
    private final String unitName;
    private final String unwrappedSource;

    /**
     * @param format          name of the format the unit is written in, for example {@code V2}
     * @param plsqlVersion    PL/SQL release that wrapped the unit: exact or a family; {@code null} if the format does
     *                        not store the version
     * @param plsqlCompatible compatibility group of PL/SQL releases; {@code null} if the format does not store the
     *                        version
     * @param staticSqlAsText whether the static SQL is kept as text, as written in the source, rather than parsed into
     *                        the tree; {@code null} if the unit has no static SQL or the format does not keep this
     * @param unitType        unit type from the header, for example {@code PACKAGE BODY}
     * @param unitName        unit name from the header; with the schema if one is given: {@code SCOTT.PKG}
     * @param unwrappedSource restored CREATE statement with the terminating {@code /} line
     */
    public UnwrapResult(String format, String plsqlVersion, String plsqlCompatible, Boolean staticSqlAsText,
            String unitType, String unitName, String unwrappedSource) {
        this.format = Objects.requireNonNull(format, "format");
        this.plsqlVersion = plsqlVersion;
        this.plsqlCompatible = plsqlCompatible;
        this.staticSqlAsText = staticSqlAsText;
        this.unitType = Objects.requireNonNull(unitType, "unitType");
        this.unitName = Objects.requireNonNull(unitName, "unitName");
        this.unwrappedSource = Objects.requireNonNull(unwrappedSource, "unwrappedSource");
    }

    /** Name of the format the unit is written in, for example {@code V2}. */
    public String format() {
        return format;
    }

    /** PL/SQL release that wrapped the unit, exact or a family; {@code null} if the format does not store it. */
    public String plsqlVersion() {
        return plsqlVersion;
    }

    /** Compatibility group of PL/SQL releases; {@code null} if the format does not store the version. */
    public String plsqlCompatible() {
        return plsqlCompatible;
    }

    /**
     * Whether the static SQL is kept as text, as written in the source, rather than parsed into the tree; {@code null}
     * if the unit has no static SQL or the format does not keep this.
     */
    public Boolean staticSqlAsText() {
        return staticSqlAsText;
    }

    /** Unit type from the header, for example {@code PACKAGE BODY}. */
    public String unitType() {
        return unitType;
    }

    /** Unit name from the header; with the schema if one is given: {@code SCOTT.PKG}. */
    public String unitName() {
        return unitName;
    }

    /** Restored CREATE statement with the terminating {@code /} line. */
    public String unwrappedSource() {
        return unwrappedSource;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof UnwrapResult)) {
            return false;
        }
        UnwrapResult that = (UnwrapResult) other;
        return format.equals(that.format) && Objects.equals(plsqlVersion, that.plsqlVersion)
                && Objects.equals(plsqlCompatible, that.plsqlCompatible)
                && Objects.equals(staticSqlAsText, that.staticSqlAsText) && unitType.equals(that.unitType)
                && unitName.equals(that.unitName) && unwrappedSource.equals(that.unwrappedSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, plsqlVersion, plsqlCompatible, staticSqlAsText, unitType, unitName,
                unwrappedSource);
    }

    @Override
    public String toString() {
        return "UnwrapResult[format=" + format + ", plsqlVersion=" + plsqlVersion + ", plsqlCompatible="
                + plsqlCompatible + ", staticSqlAsText=" + staticSqlAsText + ", unitType=" + unitType + ", unitName="
                + unitName + ", unwrappedSource=" + unwrappedSource + "]";
    }
}
