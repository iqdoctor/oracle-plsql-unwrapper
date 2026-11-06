// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap;

/** The text does not match the wrapped format, or the unit cannot be unwrapped. */
public class UnwrapException extends RuntimeException {

    public UnwrapException(String message) {
        super(message);
    }

    public UnwrapException(String message, Throwable cause) {
        super(message, cause);
    }
}
