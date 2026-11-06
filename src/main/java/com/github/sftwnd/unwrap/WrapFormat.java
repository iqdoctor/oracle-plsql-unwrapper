// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap;

/** Oracle wrapped formats the library recognizes; only {@link #V2} is unwrapped. */
public enum WrapFormat {

    /**
     * Oracle 7.2, 7.3, 8, 8i and 9i (PL/SQL 2.2, 2.3, 8.0, 8.1, 9.0–9.2): a unit holds a lexicon and a DIANA tree.
     * A unit of this format is found, but unwrapping it is not implemented and reports {@code UnwrapException}.
     */
    V1,

    /** Oracle 10g and later: a unit holds the source text normalized by WRAP and compressed. */
    V2
}
