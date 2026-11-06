// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap.v2;

import java.util.BitSet;

/**
 * Stray CRs of a V2 text. WRAP 10.2 to 11.2 for Linux read the source in blocks of 512 bytes and replace a CR right
 * before LF with a space within a block only, so of a source with CRLF a CR stays only where it stood in the last byte
 * of a block of the file or before a removed comment. Such a text has a blank before most of its line breaks and a CR
 * before a few of them. WRAP 12.1 and later keep every CR, so their text of a source with CRLF has a CR before its line
 * breaks; WRAP 10.1 leaves no CR before LF.
 * <p>
 * Outside string literals and quoted identifiers a CR separates lexemes as a space does, so the replacement does not
 * change the code; it is never removed, because a lone CR may stand between two words. Inside a literal or a quoted
 * identifier a CR is part of the value and stays.
 */
final class V2StrayCr {

    private V2StrayCr() {
    }

    /**
     * The text with every CR outside string literals and quoted identifiers replaced with a space if more line breaks
     * outside them have a blank before them than a CR, and the text itself otherwise.
     */
    static String replace(String text) {
        if (text.indexOf('\r') < 0) {
            return text;
        }
        Scan scan = new Scan(text);
        if (scan.crs.isEmpty() || scan.blankBreaks <= scan.crBreaks) {
            return text;
        }
        char[] chars = text.toCharArray();
        for (int i = scan.crs.nextSetBit(0); i >= 0; i = scan.crs.nextSetBit(i + 1)) {
            chars[i] = ' ';
        }
        return new String(chars);
    }

    /**
     * One pass over the text: the CRs outside literals and quoted identifiers, and the line breaks outside them with a
     * CR or a blank before them. Comments are outside: a quote in a hint starts nothing.
     */
    private static final class Scan {

        private final String text;
        private final BitSet crs = new BitSet();
        private int crBreaks;
        private int blankBreaks;

        private Scan(String text) {
            this.text = text;
            for (int i = 0; i < text.length(); ) {
                i = next(i);
            }
        }

        /** Takes the lexeme or the character at {@code i}; returns the offset after it. */
        private int next(int i) {
            char c = text.charAt(i);
            if (text.startsWith("/*", i)) {
                int end = text.indexOf("*/", i + 2);
                return outside(i, end < 0 ? text.length() : end + 2);
            }
            if (text.startsWith("--", i)) {
                int end = text.indexOf('\n', i);
                return outside(i, end < 0 ? text.length() : end);
            }
            if (c == '"') {
                int end = text.indexOf('"', i + 1);
                return end < 0 ? text.length() : end + 1;
            }
            if (c == '\'') {
                return literalEnd(i + 1);
            }
            if ((c == 'q' || c == 'Q') && text.startsWith("'", i + 1) && startsLexeme(i)) {
                return quotedLiteralEnd(i + 2);
            }
            return outside(i, i + 1);
        }

        /** Whether a {@code q} at {@code i} starts a literal: no identifier before it except the prefix {@code N}. */
        private boolean startsLexeme(int i) {
            if (i == 0 || !identifierPart(text.charAt(i - 1))) {
                return true;
            }
            char prefix = text.charAt(i - 1);
            return (prefix == 'n' || prefix == 'N') && (i == 1 || !identifierPart(text.charAt(i - 2)));
        }

        /** End of {@code '...'} whose content starts at {@code i}; {@code ''} stands for a quote. */
        private int literalEnd(int i) {
            for (int j = i; j < text.length(); j++) {
                if (text.charAt(j) == '\'') {
                    if (!text.startsWith("'", j + 1)) {
                        return j + 1;
                    }
                    j++;
                }
            }
            return text.length();
        }

        /** End of {@code q'x...x'} whose delimiter stands at {@code i}; brackets close with their pair. */
        private int quotedLiteralEnd(int i) {
            if (i >= text.length()) {
                return text.length();
            }
            char close = switch (text.charAt(i)) {
                case '(' -> ')';
                case '[' -> ']';
                case '{' -> '}';
                case '<' -> '>';
                default -> text.charAt(i);
            };
            int end = text.indexOf(close + "'", i + 1);
            return end < 0 ? text.length() : end + 2;
        }

        /** Counts the CRs and line breaks of {@code [from, to)}; returns {@code to}. */
        private int outside(int from, int to) {
            for (int i = from; i < to; i++) {
                char c = text.charAt(i);
                if (c == '\r') {
                    crs.set(i);
                } else if (c == '\n' && i > 0) {
                    char before = text.charAt(i - 1);
                    if (before == '\r') {
                        crBreaks++;
                    } else if (before == ' ' || before == '\t') {
                        blankBreaks++;
                    }
                }
            }
            return to;
        }

        private static boolean identifierPart(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
        }
    }
}
