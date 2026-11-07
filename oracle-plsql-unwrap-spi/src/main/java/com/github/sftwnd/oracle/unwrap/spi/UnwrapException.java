// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap.spi;

/** The text does not match the wrapped format, or the unit cannot be unwrapped. */
public class UnwrapException extends RuntimeException {

    /** An exception with the message that tells what does not match or cannot be unwrapped. */
    public UnwrapException(String message) {
        super(message);
    }

    /** An exception with the message and the error that caused it. */
    public UnwrapException(String message, Throwable cause) {
        super(message, cause);
    }
}
