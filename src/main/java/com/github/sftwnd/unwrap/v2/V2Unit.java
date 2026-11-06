// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap.v2;

import com.github.sftwnd.unwrap.UnwrapException;
import com.github.sftwnd.unwrap.spi.WrappedUnit;

import java.util.Objects;

/**
 * V2 unit; the data is decoded only on demand.
 *
 * @param start        offset of the header start
 * @param end          offset after the last Base64 line
 * @param header       the header
 * @param characterSet Oracle ID of the character set of the text
 * @param textLength   text length from the unit, the terminating NUL included
 * @param data         the Base64 data without line breaks and blanks
 * @param dataLine     line number of the first Base64 line, for error messages
 * @param newline      line break of the restored text
 * @param keepStrayCr  whether a stray CR of the text is kept ({@link V2StrayCr})
 */
record V2Unit(int start, int end, V2Header header, int characterSet, int textLength, String data, int dataLine,
              String newline, boolean keepStrayCr) implements WrappedUnit {

    V2Unit {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(newline, "newline");
    }

    @Override
    public String unitType() {
        return header.unitType();
    }

    @Override
    public String unitName() {
        return header.unitName();
    }

    /** The format does not store the PL/SQL release. */
    @Override
    public String plsqlVersion() {
        return null;
    }

    @Override
    public String plsqlCompatible() {
        return null;
    }

    /** The whole unit is source text, so the format has no other form of static SQL. */
    @Override
    public Boolean staticSqlAsText() {
        return null;
    }

    /**
     * WRAP 10.2 and later drop the blanks after the {@code /} of a unit or of an anonymous block, before plain SQL and
     * before the header of the next unit, and keep them after the {@code /} of an SQL statement. WRAP 10.1 drops them
     * only before a header, and then after a comment line too. Every release removes the comment line, so it changes
     * nothing where the blanks stay anyway.
     */
    @Override
    public boolean dropsIndentAfterSlash() {
        return true;
    }

    @Override
    public String unwrap() {
        try {
            String text = CharacterSets.decode(V2Data.text(data, textLength), characterSet);
            return V2Source.restore(header, keepStrayCr ? text : V2StrayCr.replace(text), newline);
        } catch (IllegalArgumentException e) {
            throw new UnwrapException(e.getMessage() + " (line " + dataLine + ")", e);
        }
    }
}
