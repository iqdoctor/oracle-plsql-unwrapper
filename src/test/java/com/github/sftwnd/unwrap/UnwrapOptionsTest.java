// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnwrapOptionsTest {

    /**
     * The name of the property is the package of the API followed by the option. It is written out, so that a move of
     * the package does not change it silently; this test makes such a move a decision about the property too.
     */
    @Test
    void namesThePropertyAfterThePackageAndTheOption() {
        assertEquals(UnwrapOptions.class.getPackageName() + ".keepStrayCr", UnwrapOptions.KEEP_STRAY_CR_PROPERTY);
    }

    @Test
    void replacesStrayCrByDefault() {
        withProperty(null, () -> assertFalse(UnwrapOptions.defaults().keepStrayCr()));
    }

    /** {@link Boolean#getBoolean}: {@code true} in any case, and every other value is false. */
    @ParameterizedTest
    @CsvSource({"true, true", "TRUE, true", "True, true", "false, false", "yes, false", "'', false"})
    void takesTheDefaultFromTheSystemProperty(String value, boolean keep) {
        withProperty(value, () -> assertEquals(keep, UnwrapOptions.defaults().keepStrayCr()));
    }

    /** The property sets the default of an unwrapper created after it; an option set explicitly does not use it. */
    @Test
    void keepsStrayCrOfNewUnwrappersWhenThePropertyIsTrue() {
        String wrapped = Samples.read("P1020STRAYCR.wrapped");

        withProperty("true", () -> {
            assertEquals(Samples.read("P1020STRAYCRKEPT.unwrapped"), new Unwrapper().unwrapScript(wrapped));
            assertEquals(Samples.read("P1020STRAYCR.unwrapped"),
                    new Unwrapper(UnwrapOptions.defaults().withKeepStrayCr(false)).unwrapScript(wrapped));
        });
    }

    @Test
    void changesOneValueAndKeepsTheDefaults() {
        withProperty(null, () -> {
            UnwrapOptions keep = UnwrapOptions.defaults().withKeepStrayCr(true);

            assertTrue(keep.keepStrayCr());
            assertFalse(UnwrapOptions.defaults().keepStrayCr());
            assertSame(keep, keep.withKeepStrayCr(true));
            assertEquals(UnwrapOptions.defaults(), keep.withKeepStrayCr(false));
            assertEquals(UnwrapOptions.defaults().hashCode(), keep.withKeepStrayCr(false).hashCode());
            assertNotEquals(UnwrapOptions.defaults(), keep);
        });
    }

    @Test
    void rejectsNoOptions() {
        assertThrows(NullPointerException.class, () -> new Unwrapper(null));
    }

    /** Runs {@code test} with the property set to {@code value}, or cleared for {@code null}, and restores it. */
    private static void withProperty(String value, Runnable test) {
        String saved = System.getProperty(UnwrapOptions.KEEP_STRAY_CR_PROPERTY);
        try {
            set(value);
            test.run();
        } finally {
            set(saved);
        }
    }

    private static void set(String value) {
        if (value == null) {
            System.clearProperty(UnwrapOptions.KEEP_STRAY_CR_PROPERTY);
        } else {
            System.setProperty(UnwrapOptions.KEEP_STRAY_CR_PROPERTY, value);
        }
    }
}
