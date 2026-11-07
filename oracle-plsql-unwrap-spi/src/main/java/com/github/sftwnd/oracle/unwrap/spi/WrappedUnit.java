// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap.spi;

/** Parsed wrapped unit: its bounds in the source text, header data and restoration of the text. */
public interface WrappedUnit {

    /** Offset of the header start in the source text. */
    int start();

    /**
     * Offset of the end of the parsed data, strictly greater than {@link #start()}. Script assembly itself drops
     * the unparsed rest of the unit and the {@code /} line that terminates the statement.
     */
    int end();

    /** Unit type from the header, for example {@code PACKAGE BODY}. */
    String unitType();

    /** Unit name from the header, with the schema if one is given. */
    String unitName();

    /**
     * PL/SQL release that wrapped the unit, as precisely as the unit itself allows: an exact release, for example
     * {@code 2.2.2.3.1}, or a family, for example {@code 2.3.x}; {@code null} if the format does not store the version.
     */
    String plsqlVersion();

    /**
     * Compatibility group of PL/SQL releases, for example {@code 2.x} for PL/SQL 2.2 and 2.3; {@code null} if the
     * format does not store the version.
     */
    String plsqlCompatible();

    /**
     * Whether the static SQL of the unit is kept as text, as written in the source, rather than parsed into the tree;
     * {@code null} if the unit has no static SQL or the format does not keep this.
     */
    Boolean staticSqlAsText();

    /**
     * Whether WRAP of this format may drop the blanks at the start of a line that follows a {@code /} line, unless a
     * {@code --} comment line stands between them: WRAP removes a comment line right after a {@code /} line and keeps
     * the blanks after it. Script assembly then writes such a comment line before each line that follows a {@code /}
     * line and starts with blanks, from the end of this unit up to the first line of the next unit.
     */
    default boolean dropsIndentAfterSlash() {
        return false;
    }

    /**
     * Restores the CREATE statement with the terminating {@code /} line. Lines are separated by the line break that
     * ends the header line of the unit in the source text (CRLF, LF or CR), or by the line separator of the operating
     * system if the header line has none.
     *
     * @throws UnwrapException if the unit content cannot be unwrapped
     */
    String unwrap();
}
