// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap.v2;

import com.github.sftwnd.oracle.unwrap.spi.UnwrapException;
import com.github.sftwnd.oracle.unwrap.spi.WrappedUnit;

import java.util.Objects;

/**
 * V2 unit; the data is decoded only on demand.
 */
final class V2Unit implements WrappedUnit {

    private final int start;
    private final int end;
    private final V2Header header;
    private final int characterSet;
    private final int textLength;
    private final String data;
    private final int dataLine;
    private final String newline;
    private final boolean keepStrayCr;

    /**
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
    V2Unit(int start, int end, V2Header header, int characterSet, int textLength, String data, int dataLine,
            String newline, boolean keepStrayCr) {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(newline, "newline");
        this.start = start;
        this.end = end;
        this.header = header;
        this.characterSet = characterSet;
        this.textLength = textLength;
        this.data = data;
        this.dataLine = dataLine;
        this.newline = newline;
        this.keepStrayCr = keepStrayCr;
    }

    @Override
    public int start() {
        return start;
    }

    @Override
    public int end() {
        return end;
    }

    V2Header header() {
        return header;
    }

    int characterSet() {
        return characterSet;
    }

    int textLength() {
        return textLength;
    }

    String data() {
        return data;
    }

    int dataLine() {
        return dataLine;
    }

    String newline() {
        return newline;
    }

    boolean keepStrayCr() {
        return keepStrayCr;
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
