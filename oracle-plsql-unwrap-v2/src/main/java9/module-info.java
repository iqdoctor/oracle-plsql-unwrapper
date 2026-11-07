// SPDX-License-Identifier: BSD-3-Clause

/** Format V2 (Oracle 10g and later); it is reached through the service only, so nothing is exported. */
@SuppressWarnings("module") // The digit names the format; it is no version of the module
module com.github.sftwnd.oracle.unwrap.v2 {
    requires com.github.sftwnd.oracle.unwrap.spi;

    provides com.github.sftwnd.oracle.unwrap.spi.FormatUnwrapper
            with com.github.sftwnd.oracle.unwrap.v2.V2FormatUnwrapper;
}
