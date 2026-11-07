// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap;

import com.github.sftwnd.oracle.unwrap.spi.WrappedUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SQL script assembly common to all formats: wrapped units are replaced with the restored text, the rest of the
 * text is copied unchanged, line endings included. Nothing is appended to the end of the script.
 * <p>
 * After a unit whose WRAP may drop the blanks at the start of a line that follows a {@code /} line (see
 * {@link WrappedUnit#dropsIndentAfterSlash()}), a {@code --} line is written before each such line: the blanks may
 * have stayed only because of a comment line that WRAP removed, and WRAP removes the written one as well.
 * <p>
 * A format parses a unit only up to the end of the data it needs. The rest of the unit and the {@code /} line that
 * terminates the statement are dropped because the restored text carries its own terminator. If that {@code /} line
 * ends the input without a line break, the terminator of the restored text loses its line break too.
 */
final class ScriptUnwrapper {

    private final String input;
    private final List<WrappedUnit> units;

    /** Units must follow in text order and must not overlap. */
    ScriptUnwrapper(String input, List<WrappedUnit> units) {
        this.input = Objects.requireNonNull(input, "input");
        this.units = new ArrayList<>(units);
    }

    String unwrap() {
        StringBuilder output = new StringBuilder(input.length());
        int position = 0;
        WrappedUnit previous = null;
        for (WrappedUnit unit : units) {
            // A unit may start on the line where the previous one ends
            int unitStart = Math.max(position, lineStart(unit.start()));
            boolean comments = false;
            if (previous != null) {
                position = statementEnd(position, unitStart);
                comments = previous.dropsIndentAfterSlash();
            }
            appendText(output, input.substring(position, unitStart), comments);
            String restored = unit.unwrap();
            if (comments) {
                commentIndent(output, restored, 0);
            }
            output.append(restored);
            position = unit.end();
            previous = unit;
        }
        if (previous == null) {
            return input;
        }
        int rest = statementEnd(position, input.length());
        if (rest == input.length() && endsWithSlashLine(position)) {
            String lineBreak = slashLineBreak(output);
            if (lineBreak != null) {
                output.setLength(output.length() - lineBreak.length());
            }
            return output.toString();
        }
        appendText(output, input.substring(rest), previous.dropsIndentAfterSlash());
        return output.toString();
    }

    /** Whether the input ends with a {@code /} line without a line break that starts at {@code from} or later. */
    private boolean endsWithSlashLine(int from) {
        int start = lineStart(input.length());
        return start >= from && trimSpaces(input.substring(start)).equals("/");
    }

    /** Appends the text; with {@code comments}, a comment line goes before every indented line after a {@code /}. */
    private static void appendText(StringBuilder output, String text, boolean comments) {
        if (!comments) {
            output.append(text);
            return;
        }
        int start = 0;
        while (start < text.length()) {
            int end = text.indexOf('\n', start) + 1;
            if (end == 0) {
                end = text.length();
            }
            commentIndent(output, text, start);
            output.append(text, start, end);
            start = end;
        }
    }

    /**
     * Writes a {@code --} line, with the line break of the {@code /} line that ends the output, if the line of the
     * text at {@code start} begins with blanks and holds more than blanks.
     */
    private static void commentIndent(StringBuilder output, String text, int start) {
        int end = start;
        while (end < text.length() && (text.charAt(end) == ' ' || text.charAt(end) == '\t')) {
            end++;
        }
        if (end == start || end == text.length() || text.charAt(end) == '\r' || text.charAt(end) == '\n') {
            return;
        }
        String lineBreak = slashLineBreak(output);
        if (lineBreak != null) {
            output.append("--").append(lineBreak);
        }
    }

    /** Line break of the {@code /} line that ends the output, or {@code null} if the output does not end with one. */
    private static String slashLineBreak(StringBuilder output) {
        int end = output.length();
        if (end == 0 || output.charAt(end - 1) != '\n') {
            return null;
        }
        String lineBreak = end > 1 && output.charAt(end - 2) == '\r' ? "\r\n" : "\n";
        int lineEnd = end - lineBreak.length();
        int lineStart = output.lastIndexOf("\n", lineEnd - 1) + 1;
        return trimSpaces(output.substring(lineStart, lineEnd)).equals("/") ? lineBreak : null;
    }

    /** Start of the line that contains the offset. */
    private int lineStart(int offset) {
        return input.lastIndexOf('\n', offset - 1) + 1;
    }

    /**
     * End of the statement holding a wrapped unit: the offset after the {@code /} line or the start of the line
     * with the next CREATE. Only lines terminated by a line feed and lying before {@code limit} are checked.
     *
     * @return the offset to continue from, or {@code limit} if the statement does not end before it: the rest of
     *         the unit then extends up to {@code limit}
     */
    private int statementEnd(int from, int limit) {
        int start = from;
        for (int end = input.indexOf('\n', start); end >= 0 && end < limit; end = input.indexOf('\n', start)) {
            String line = trimSpaces(withoutCarriageReturn(input.substring(start, end)));
            if (line.equals("/")) {
                return end + 1;
            }
            if (line.regionMatches(true, 0, "CREATE", 0, 6)) {
                return start;
            }
            start = end + 1;
        }
        return limit;
    }

    private static String withoutCarriageReturn(String line) {
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }

    private static String trimSpaces(String line) {
        int start = 0;
        int end = line.length();
        while (start < end && (line.charAt(start) == ' ' || line.charAt(start) == '\t')) {
            start++;
        }
        while (end > start && (line.charAt(end - 1) == ' ' || line.charAt(end - 1) == '\t')) {
            end--;
        }
        return line.substring(start, end);
    }
}
