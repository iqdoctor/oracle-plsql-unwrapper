// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap.v2;

import com.github.sftwnd.oracle.unwrap.spi.FormatUnwrapper;
import com.github.sftwnd.oracle.unwrap.spi.SourceLines;
import com.github.sftwnd.oracle.unwrap.spi.UnwrapOptions;
import com.github.sftwnd.oracle.unwrap.spi.WrappedUnit;

/**
 * Format V2 (Oracle 10g and later): the unit holds the source text normalized by WRAP, compressed and enciphered by a
 * byte substitution; the text is restored with the header of the unit.
 */
public final class V2FormatUnwrapper implements FormatUnwrapper {

    private static final String FORMAT = "V2";

    private final V2UnitParser parser;

    /** A stray CR of the text is kept or replaced as {@link UnwrapOptions#defaults()} decide. */
    public V2FormatUnwrapper() {
        this(UnwrapOptions.defaults().keepStrayCr());
    }

    /** @param keepStrayCr whether a stray CR of the text is kept as the unit holds it ({@code V2StrayCr}) */
    public V2FormatUnwrapper(boolean keepStrayCr) {
        parser = new V2UnitParser(keepStrayCr);
    }

    @Override
    public String format() {
        return FORMAT;
    }

    @Override
    public String description() {
        return "Oracle 10g and later";
    }

    @Override
    public FormatUnwrapper withOptions(UnwrapOptions options) {
        return new V2FormatUnwrapper(options.keepStrayCr());
    }

    @Override
    public int find(String text, int from) {
        SourceLines.checkOffset(text, from);
        return parser.find(text, from);
    }

    @Override
    public WrappedUnit read(String text, int start) {
        SourceLines.checkOffset(text, start);
        return parser.read(text, start);
    }
}
