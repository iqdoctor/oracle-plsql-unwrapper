// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap.v2;

import java.util.Locale;

/**
 * Restored CREATE statement of a V2 unit. The text of the unit is the source normalized by WRAP: it starts with the
 * kind and the name without CREATE and the schema, unquoted words are in upper case and comments are removed. The
 * header is the only place that keeps CREATE, OR REPLACE, IF NOT EXISTS, the schema, the spelling of the name and the
 * SHARING, TIMESTAMP and OID clauses, so the statement is the header followed by the text after its kind and name.
 */
final class V2Source {

    private static final String EMPTY_COMMENT = "/**/";

    private V2Source() {
    }

    /**
     * The statement with the terminating {@code /}; the line breaks of the text become {@code newline}.
     * <p>
     * A text that ends with a blank or a line break gets an empty block comment after it. WRAP 10.2 and later end the
     * text with the last token and keep the blanks after it only before a removed comment; WRAP 10.1 keeps everything
     * up to the {@code /} of the source or up to a removed comment. Every release removes a block comment at the end of
     * the text together with the line break after it, so the text comes back unchanged. A {@code --} comment would not
     * do: WRAP 10.1 keeps the line break after it.
     *
     * @throws IllegalArgumentException if the text does not start with the kind and a name
     */
    static String restore(V2Header header, String text, String newline) {
        int offset = skipSpaces(text, 0);
        for (String word : header.unitType().split(" ")) {
            int end = identifierEnd(text, offset);
            if (!text.substring(offset, end).equalsIgnoreCase(word)) {
                throw new IllegalArgumentException("Text of the V2 unit does not start with " + header.unitType());
            }
            offset = skipSpaces(text, end);
        }
        int nameEnd;
        boolean glued = false;
        if (offset < text.length() && text.charAt(offset) == '"') {
            nameEnd = text.indexOf('"', offset + 1) + 1;
            if (nameEnd == 0) {
                throw new IllegalArgumentException("Unterminated unit name in the text of the V2 unit");
            }
        } else {
            nameEnd = identifierEnd(text, offset);
            if (nameEnd == offset) {
                throw new IllegalArgumentException("No unit name in the text of the V2 unit");
            }
            // WRAP removes a comment without a separator: "tz_obj/* c */as" becomes "tz_objAS"
            String name = header.simpleName();
            if (nameEnd - offset > name.length()
                    && text.substring(offset, offset + name.length()).toUpperCase(Locale.ROOT).equals(name)) {
                nameEnd = offset + name.length();
                glued = true;
            }
        }
        StringBuilder source = new StringBuilder(text.length() + header.text().length() + 32);
        String headerText = header.text();
        if (!header.create()) {
            int indent = skipBlanks(headerText);
            source.append(headerText, 0, indent).append("CREATE OR REPLACE ");
            headerText = headerText.substring(indent);
        }
        source.append(headerText);
        if (glued) {
            source.append(' ');
        }
        int textStart = nameEnd;
        // WRAP 10.1 leaves spaces in the text in place of the clauses that the header holds
        if (header.clausesLength() > 0 && skipSpaces(text, nameEnd) >= nameEnd + header.clausesLength()) {
            appendLines(source, header.clauseEnd(), 0, newline);
            textStart += header.clausesLength();
        }
        appendLines(source, text, textStart, newline);
        if (endsWithBlank(text)) {
            source.append(EMPTY_COMMENT);
        }
        return source.append(newline).append('/').append(newline).toString();
    }

    private static boolean endsWithBlank(String text) {
        if (text.isEmpty()) {
            return false;
        }
        char last = text.charAt(text.length() - 1);
        return last == ' ' || last == '\t' || last == '\n' || last == '\r';
    }

    /**
     * Appends the text from {@code start} with LF replaced by {@code newline}; a lone CR is kept. A CRLF stays in a
     * unit with LF: WRAP 12.1 and later keep the CR of the source, in string literals too, so it is part of the text.
     */
    private static void appendLines(StringBuilder source, String text, int start, String newline) {
        String crlf = newline.equals("\n") ? "\r\n" : newline;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                source.append(crlf);
                i++;
            } else if (c == '\n') {
                source.append(newline);
            } else {
                source.append(c);
            }
        }
    }

    private static int skipBlanks(String text) {
        int end = 0;
        while (end < text.length() && (text.charAt(end) == ' ' || text.charAt(end) == '\t')) {
            end++;
        }
        return end;
    }

    private static int skipSpaces(String text, int offset) {
        int end = offset;
        while (end < text.length() && Character.isWhitespace(text.charAt(end))) {
            end++;
        }
        return end;
    }

    private static int identifierEnd(String text, int offset) {
        int end = offset;
        while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_'
                || text.charAt(end) == '$' || text.charAt(end) == '#')) {
            end++;
        }
        return end;
    }
}
