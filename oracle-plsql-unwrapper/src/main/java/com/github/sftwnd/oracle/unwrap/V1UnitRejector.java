// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap;

import com.github.sftwnd.oracle.unwrap.spi.FormatUnwrapper;
import com.github.sftwnd.oracle.unwrap.spi.SourceLines;
import com.github.sftwnd.oracle.unwrap.spi.UnwrapException;
import com.github.sftwnd.oracle.unwrap.spi.WrappedUnit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stands in for the format V1 (Oracle 7.2-9i) when no implementation of it is present: a V1 unit is found so that it
 * does not stay wrapped in a restored script without an error, and {@link #read} reports where to get a license.
 */
final class V1UnitRejector implements FormatUnwrapper {

    /** Name of the format; an implementation with this name replaces the rejector. */
    static final String FORMAT = "V1";

    static final String NOT_IMPLEMENTED = "Unwrapping of the wrapped format V1 (Oracle 7.2-9i) is not implemented. "
            + "To get a license for the closed module that unwraps V1, contact the author: ashindarev@gmail.com, "
            + "https://www.linkedin.com/in/ashindarev/";

    /**
     * Header of a V1 unit: the {@code CREATE} statement up to the name and the word {@code wrapped}, then the lines
     * {@code 0} and {@code abcd}. A V2 unit has {@code a000000} instead of {@code 0}. The distance to {@code wrapped}
     * is limited so that a {@code CREATE} that starts no unit is not searched for to the end of the text.
     */
    private static final Pattern HEADER = Pattern.compile(
            "^[ \\t]*(CREATE\\b[\\s\\S]{0,1024}?\\bWRAPPED[ \\t]*"
                    + "(?:\\r\\n|\\n|\\r)0[ \\t]*(?:\\r\\n|\\n|\\r)abcd)[ \\t]*$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    @Override
    public String format() {
        return FORMAT;
    }

    @Override
    public String description() {
        return "Oracle 7.2-9i, not implemented";
    }

    @Override
    public int find(String text, int from) {
        SourceLines.checkOffset(text, from);
        Matcher matcher = HEADER.matcher(text);
        return matcher.find(from) ? matcher.start(1) : -1;
    }

    @Override
    public WrappedUnit read(String text, int start) {
        SourceLines.checkOffset(text, start);
        throw new UnwrapException("Line " + SourceLines.lineNumber(text, start) + ": " + NOT_IMPLEMENTED);
    }
}
