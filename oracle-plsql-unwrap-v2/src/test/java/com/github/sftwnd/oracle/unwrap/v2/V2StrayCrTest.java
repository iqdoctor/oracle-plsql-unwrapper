// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap.v2;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class V2StrayCrTest {

    /** Lines of WRAP 10.2 to 11.2 from a source with CRLF: a blank before each line break. */
    private static final String BLANK_LINES = "PROCEDURE P IS \n  L VARCHAR2(10); \nBEGIN \n";

    static Stream<Arguments> texts() {
        return Stream.of(
                // a CR at the end of a block of the source stays before LF
                arguments(BLANK_LINES + "  NULL;\r\nEND; \n", BLANK_LINES + "  NULL; \nEND; \n"),
                // a lone CR between two words becomes a space and is not removed
                arguments(BLANK_LINES + "  NULL;\rNULL; \nEND; \n", BLANK_LINES + "  NULL; NULL; \nEND; \n"),
                // inside literals and quoted identifiers the CR is part of the value
                arguments(BLANK_LINES + "  L := 'A\r\nB'; \n", null),
                arguments(BLANK_LINES + "  L := 'IT''S\r\nB'; \n", null),
                arguments(BLANK_LINES + "  L := N'A\r\nB'; \n", null),
                arguments(BLANK_LINES + "  L := q'[A'\r\nB]'; \n", null),
                arguments(BLANK_LINES + "  L := Q'{A\r\nB}'; \n", null),
                arguments(BLANK_LINES + "  L := nq'<A\r\nB>'; \n", null),
                arguments(BLANK_LINES + "  L := q'!A\r\nB!'; \n", null),
                arguments(BLANK_LINES + "  \"Q\rID\" := 1; \n", null),
                arguments(BLANK_LINES + "  L := 'A\r\n", null),
                // after a literal the CR is stray again
                arguments(BLANK_LINES + "  L := q'[A\r\nB]';\r\n", BLANK_LINES + "  L := q'[A\r\nB]'; \n"),
                // an identifier ending with Q does not start a q-quoted literal
                arguments(BLANK_LINES + "  L := XQ'['\r\n||']'; \n", BLANK_LINES + "  L := XQ'[' \n||']'; \n"),
                // a quote in a hint starts nothing
                arguments(BLANK_LINES + "  SELECT /*+ ' */ 1\r\n  INTO L FROM DUAL; \n",
                        BLANK_LINES + "  SELECT /*+ ' */ 1 \n  INTO L FROM DUAL; \n"),
                arguments(BLANK_LINES + "  SELECT --+ '\r\n  1 INTO L FROM DUAL; \n",
                        BLANK_LINES + "  SELECT --+ ' \n  1 INTO L FROM DUAL; \n"),
                // WRAP 12.1 and later keep every CR of a source with CRLF
                arguments("PROCEDURE P IS\r\nBEGIN\r\n  NULL; \nEND;\r\n", null),
                // as many CRs as blanks before line breaks: not the trace of WRAP 10.2 to 11.2
                arguments("PROCEDURE P IS \nBEGIN\r\nEND;", null),
                arguments("PROCEDURE P IS\nBEGIN\nEND;", null));
    }

    /** {@code expected} is {@code null} where the text stays as it is. */
    @ParameterizedTest
    @MethodSource("texts")
    void replacesStrayCrWithSpace(String text, String expected) {
        assertEquals(expected == null ? text : expected, V2StrayCr.replace(text));
    }
}
