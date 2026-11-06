// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap.spi;

import com.github.sftwnd.unwrap.UnwrapException;
import com.github.sftwnd.unwrap.WrapFormat;

/**
 * Implementation of one wrapped format: finds units in the text and parses them.
 * <p>
 * The {@code Unwrapper} facade queries all formats and takes the nearest unit, so one script may contain units
 * of different formats. An implementation must be thread-safe.
 */
public interface FormatUnwrapper {

    WrapFormat format();

    /**
     * Finds the header of the next unit of this format. A unit header starts on a new line; only spaces and tabs
     * may precede it. A format may also return a unit whose header it cannot read, for {@link #read} to report it:
     * such a unit does not stay wrapped in a restored script without an error.
     *
     * @return offset of the header start not before {@code from}, or -1 if there are no more units of this format
     */
    int find(String text, int from);

    /**
     * Parses the unit whose header {@link #find} has found at offset {@code start}.
     *
     * @throws UnwrapException if the unit content does not match the format
     */
    WrappedUnit read(String text, int start);
}
