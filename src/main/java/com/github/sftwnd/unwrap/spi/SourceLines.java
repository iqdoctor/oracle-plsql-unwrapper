// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap.spi;

import java.util.Objects;

/** Lines of the source text as all formats count them: CRLF, LF and CR each end a line. */
public final class SourceLines {

    private SourceLines() {
    }

    /**
     * Line break that ends the line holding the offset {@code start}: CRLF, LF or CR. The restored text of a unit uses
     * the one of its header line, so that it matches the rest of the script; without one it is the line separator of
     * the operating system.
     */
    public static String lineBreak(String text, int start) {
        Objects.requireNonNull(text, "text");
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                return "\n";
            }
            if (c == '\r') {
                return i + 1 < text.length() && text.charAt(i + 1) == '\n' ? "\r\n" : "\r";
            }
        }
        return System.lineSeparator();
    }

    /** Number of the line holding the offset, counted from 1. */
    public static int lineNumber(String text, int offset) {
        Objects.requireNonNull(text, "text");
        int line = 1;
        for (int i = 0; i < offset; i++) {
            char c = text.charAt(i);
            if (c == '\n' || (c == '\r' && (i + 1 >= text.length() || text.charAt(i + 1) != '\n'))) {
                line++;
            }
        }
        return line;
    }
}
