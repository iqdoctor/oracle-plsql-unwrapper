// SPDX-License-Identifier: BSD-3-Clause

/** Restores PL/SQL source code from the Oracle wrapped formats that are present as services of the SPI. */
module com.github.sftwnd.oracle.unwrap {
    requires transitive com.github.sftwnd.oracle.unwrap.spi;

    exports com.github.sftwnd.oracle.unwrap;

    uses com.github.sftwnd.oracle.unwrap.spi.FormatUnwrapper;
}
