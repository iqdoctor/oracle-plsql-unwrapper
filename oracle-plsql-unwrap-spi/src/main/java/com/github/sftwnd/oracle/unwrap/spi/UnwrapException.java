// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap.spi;

/** The text does not match the wrapped format, or the unit cannot be unwrapped. */
public class UnwrapException extends RuntimeException {

    public UnwrapException(String message) {
        super(message);
    }

    public UnwrapException(String message, Throwable cause) {
        super(message, cause);
    }
}
