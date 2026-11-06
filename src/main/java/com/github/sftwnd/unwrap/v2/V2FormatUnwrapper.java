// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap.v2;

import com.github.sftwnd.unwrap.UnwrapOptions;
import com.github.sftwnd.unwrap.WrapFormat;
import com.github.sftwnd.unwrap.spi.FormatUnwrapper;
import com.github.sftwnd.unwrap.spi.WrappedUnit;

import java.util.Objects;

/**
 * Format V2 (Oracle 10g and later): the unit holds the source text normalized by WRAP, compressed and enciphered by a
 * byte substitution; the text is restored with the header of the unit.
 */
public final class V2FormatUnwrapper implements FormatUnwrapper {

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
    public WrapFormat format() {
        return WrapFormat.V2;
    }

    @Override
    public int find(String text, int from) {
        Objects.requireNonNull(text, "text");
        Objects.checkIndex(from, text.length() + 1);
        return parser.find(text, from);
    }

    @Override
    public WrappedUnit read(String text, int start) {
        Objects.requireNonNull(text, "text");
        Objects.checkIndex(start, text.length() + 1);
        return parser.read(text, start);
    }
}
