// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap.spi;

/**
 * Options of the unwrapper; a format takes those it needs in {@link FormatUnwrapper#withOptions}. The object is
 * immutable: each {@code with} method returns options with one value changed, so options can be added later without
 * breaking the code that sets the others.
 */
public final class UnwrapOptions {

    /**
     * System property with the value of {@link #keepStrayCr()} in {@link #defaults()}:
     * {@code -Dcom.github.sftwnd.oracle.unwrap.keepStrayCr=true}.
     */
    public static final String KEEP_STRAY_CR_PROPERTY = "com.github.sftwnd.oracle.unwrap.keepStrayCr";

    private static final UnwrapOptions REPLACE_STRAY_CR = new UnwrapOptions(false);
    private static final UnwrapOptions KEEP_STRAY_CR = new UnwrapOptions(true);

    private final boolean keepStrayCr;

    private UnwrapOptions(boolean keepStrayCr) {
        this.keepStrayCr = keepStrayCr;
    }

    /**
     * The defaults: a stray CR of a V2 text becomes a space unless the system property {@value #KEEP_STRAY_CR_PROPERTY}
     * is {@code true}, in any case. The property is read on each call, so it applies to the unwrappers created after it
     * is set; options set by a {@code with} method do not depend on it.
     */
    public static UnwrapOptions defaults() {
        return of(Boolean.getBoolean(KEEP_STRAY_CR_PROPERTY));
    }

    /**
     * Whether a stray CR of a V2 text is kept as the unit holds it. WRAP 10.2 to 11.2 replace the CR of a source with
     * CRLF with a space, except where it stood in the last byte of a block of 512 bytes of the source file; such a CR
     * outside string literals and quoted identifiers is stray, and by default it becomes a space (see
     * {@link #defaults()}).
     */
    public boolean keepStrayCr() {
        return keepStrayCr;
    }

    /** These options with {@link #keepStrayCr()} set to {@code keep}. */
    public UnwrapOptions withKeepStrayCr(boolean keep) {
        return of(keep);
    }

    private static UnwrapOptions of(boolean keepStrayCr) {
        return keepStrayCr ? KEEP_STRAY_CR : REPLACE_STRAY_CR;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UnwrapOptions && ((UnwrapOptions) other).keepStrayCr == keepStrayCr;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(keepStrayCr);
    }

    @Override
    public String toString() {
        return "UnwrapOptions[keepStrayCr=" + keepStrayCr + "]";
    }
}
