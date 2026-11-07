// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The distribution with the format V2 only. */
class UnwrapperLaunchIT extends AbstractUnwrapperLaunchIT {

    @Override
    protected String sample() {
        return "P1020EDGE";
    }

    @Override
    protected String format() {
        return "V2";
    }

    @Override
    protected void assertHelp(String help) {
        assertTrue(help.contains("  V2  Oracle 10g and later\n"), help);
        assertFalse(help.contains("V1"), help);
    }
}
