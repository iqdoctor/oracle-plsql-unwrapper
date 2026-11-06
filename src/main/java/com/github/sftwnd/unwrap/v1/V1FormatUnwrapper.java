// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap.v1;

import com.github.sftwnd.unwrap.UnwrapException;
import com.github.sftwnd.unwrap.WrapFormat;
import com.github.sftwnd.unwrap.spi.FormatUnwrapper;
import com.github.sftwnd.unwrap.spi.SourceLines;
import com.github.sftwnd.unwrap.spi.WrappedUnit;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Format V1 (Oracle 7.2–9i, PL/SQL 2.2–9.2): the units are recognized, but not unwrapped.
 * <p>
 * A V1 unit is found so that it does not stay wrapped in a restored script without an error, and {@link #read}
 * reports that the code is not implemented and where to get it.
 */
public final class V1FormatUnwrapper implements FormatUnwrapper {

    static final String NOT_IMPLEMENTED = "Unwrapping of the wrapped format V1 (Oracle 7.2-9i) is not implemented. "
            + "To get the code that unwraps V1, contact the author: ashindarev@gmail.com, "
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
    public WrapFormat format() {
        return WrapFormat.V1;
    }

    @Override
    public int find(String text, int from) {
        Objects.requireNonNull(text, "text");
        Objects.checkIndex(from, text.length() + 1);
        Matcher matcher = HEADER.matcher(text);
        return matcher.find(from) ? matcher.start(1) : -1;
    }

    @Override
    public WrappedUnit read(String text, int start) {
        Objects.requireNonNull(text, "text");
        Objects.checkIndex(start, text.length() + 1);
        throw new UnwrapException("Line " + SourceLines.lineNumber(text, start) + ": " + NOT_IMPLEMENTED);
    }
}
