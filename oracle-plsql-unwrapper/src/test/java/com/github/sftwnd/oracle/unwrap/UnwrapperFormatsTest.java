// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap;

import com.github.sftwnd.oracle.unwrap.spi.UnwrapException;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The formats of this module: V2 comes through the service, a V1 unit is reported, not left in the script. */
class UnwrapperFormatsTest {

    private static final String V1_UNIT = "SELECT 1 FROM dual;\ncreate package p wrapped\n0\nabcd\nabcd\n/\n";

    private final Unwrapper unwrapper = new Unwrapper();

    @Test
    void findsTheFormatV2ThroughTheService() {
        assertEquals(Collections.singletonMap("V2", "Oracle 10g and later"), unwrapper.formats());
    }

    @Test
    void recognizesV1UnitButDoesNotRestoreIt() {
        assertTrue(unwrapper.supports(V1_UNIT));

        UnwrapException e = assertThrows(UnwrapException.class, () -> unwrapper.unwrapScript(V1_UNIT));

        assertEquals("Line 2: " + V1UnitRejector.NOT_IMPLEMENTED, e.getMessage());
    }

    @Test
    void reportsV1UnitOfEachOperation() {
        assertThrows(UnwrapException.class, () -> unwrapper.unwrap(V1_UNIT));
        assertThrows(UnwrapException.class, () -> unwrapper.unwrapUnits(V1_UNIT));
    }
}
