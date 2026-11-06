// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap;

import com.github.sftwnd.unwrap.spi.WrappedUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Script assembly over stub units: the unit bounds are set explicitly, the restored text is a marker. */
class ScriptUnwrapperTest {

    @Test
    void replacesUnitsAndCopiesTextBetweenThem() {
        String script = "-- head\nCREATE U1 wrapped\nDATA1\n1 2 3\n/\n-- between\nCREATE U2 wrapped\nDATA2\n4 5 6";

        String result = unwrap(script,
                unit(script, "CREATE U1", "DATA1", "<U1>"), unit(script, "CREATE U2", "DATA2", "<U2>"));

        // The rest of the unit data up to the "/" line is dropped together with that line
        assertEquals("-- head\n<U1>-- between\n<U2>", result);
    }

    @Test
    void keepsLineEndingsOfText() {
        String script = "line 1\r\nline 2\nline 3\r\r\nCREATE U1 wrapped\nDATA1\n/\r\nline 4\nline 5\r";

        assertEquals("line 1\r\nline 2\nline 3\r\r\n<U1>line 4\nline 5\r",
                unwrap(script, unit(script, "CREATE U1", "DATA1", "<U1>")));
    }

    @Test
    void dropsIndentOfUnitHeaderLine() {
        String script = "-- head\n   CREATE U1 wrapped\nDATA1";

        assertEquals("-- head\n<U1>", unwrap(script, unit(script, "CREATE U1", "DATA1", "<U1>")));
    }

    @Test
    void keepsTerminatorWithSpacesOutOfOutput() {
        String script = "CREATE U1 wrapped\nDATA1\n \t/ \n-- after\nCREATE U2 wrapped\nDATA2";

        String result = unwrap(script,
                unit(script, "CREATE U1", "DATA1", "<U1>"), unit(script, "CREATE U2", "DATA2", "<U2>"));

        assertEquals("<U1>-- after\n<U2>", result);
    }

    @Test
    void unitWithoutTerminatorEndsAtNextCreate() {
        String script = "CREATE U1 wrapped\nDATA1\n1 2 3\ncreate table t (a number);\nCREATE U2 wrapped\nDATA2";

        String result = unwrap(script,
                unit(script, "CREATE U1", "DATA1", "<U1>"), unit(script, "CREATE U2", "DATA2", "<U2>"));

        assertEquals("<U1>create table t (a number);\n<U2>", result);
    }

    @Test
    void keepsAllLinesOfPlainCreateStatements() {
        String script = "CREATE OR REPLACE PROCEDURE p\n(a NUMBER) IS\nBEGIN NULL; END;\n/\nCREATE U1 wrapped\nDATA1";

        assertEquals("CREATE OR REPLACE PROCEDURE p\n(a NUMBER) IS\nBEGIN NULL; END;\n/\n<U1>",
                unwrap(script, unit(script, "CREATE U1", "DATA1", "<U1>")));
    }

    /** WRAP 10.2 and later keep a source that ends with {@code /} without a line break; so does the script. */
    @Test
    void keepsMissingLineBreakAfterLastTerminator() {
        String script = "CREATE U1 wrapped\nDATA1\n\n/\nCREATE U2 wrapped\nDATA2\n\n/";

        String result = unwrap(script,
                unit(script, "CREATE U1", "DATA1", "U1\n/\n"), unit(script, "CREATE U2", "DATA2", "U2\n/\n"));

        assertEquals("U1\n/\nU2\n/", result);
    }

    @Test
    void keepsMissingLineBreakAfterLastTerminatorWithCrlf() {
        String script = "CREATE U1 wrapped\r\nDATA1\r\n\r\n/";

        assertEquals("U1\r\n/", unwrap(script, unit(script, "CREATE U1", "DATA1", "U1\r\n/\r\n")));
    }

    @Test
    void keepsLineBreakAfterLastTerminator() {
        String script = "CREATE U1 wrapped\nDATA1\n\n/\n";

        assertEquals("U1\n/\n", unwrap(script, unit(script, "CREATE U1", "DATA1", "U1\n/\n")));
    }

    /** A {@code /} without a line break that ends plain SQL after the last unit is copied with the rest of the text. */
    @Test
    void copiesLastTerminatorOfPlainText() {
        String script = "CREATE U1 wrapped\nDATA1\n/\nbegin null; end;\n/";

        assertEquals("U1\n/\nbegin null; end;\n/", unwrap(script, unit(script, "CREATE U1", "DATA1", "U1\n/\n")));
    }

    /**
     * After a unit whose WRAP drops the blanks after a {@code /} line, a comment line goes before every line that
     * follows a {@code /} line and starts with blanks; other lines and lines of blanks stay as they are.
     */
    @Test
    void writesCommentLineBeforeIndentedLineAfterSlash() {
        String script = "CREATE U1 wrapped\nDATA1\n/\n  grant a;\n  more;\nbegin null; end;\n/\n\tgrant b;\n/\n  \n/";

        assertEquals("U1\n/\n--\n  grant a;\n  more;\nbegin null; end;\n/\n--\n\tgrant b;\n/\n  \n/",
                unwrap(script, droppingUnit(script, "CREATE U1", "DATA1", "U1\n/\n")));
    }

    @Test
    void writesCommentLineBeforeIndentedHeaderOfNextUnit() {
        String script = "CREATE U1 wrapped\nDATA1\n/\n  CREATE U2 wrapped\nDATA2\n/\n";

        String result = unwrap(script, droppingUnit(script, "CREATE U1", "DATA1", "U1\n/\n"),
                droppingUnit(script, "CREATE U2", "DATA2", "  U2\n/\n  inner\n"));

        assertEquals("U1\n/\n--\n  U2\n/\n  inner\n", result);
    }

    @Test
    void writesCommentLineWithLineBreakOfSlashLine() {
        String script = "CREATE U1 wrapped\r\nDATA1\r\n/\r\nbegin null; end;\r\n/\r\n  grant a;\r\n";

        assertEquals("U1\r\n/\r\nbegin null; end;\r\n/\r\n--\r\n  grant a;\r\n",
                unwrap(script, droppingUnit(script, "CREATE U1", "DATA1", "U1\r\n/\r\n")));
    }

    /** Text before the first unit and after a unit of a format that keeps the blanks is copied unchanged. */
    @Test
    void keepsIndentedLinesOutsideTextAfterDroppingUnit() {
        String script = "begin null; end;\n/\n  grant a;\nCREATE U1 wrapped\nDATA1\n/\n  grant b;\n";

        assertEquals("begin null; end;\n/\n  grant a;\nU1\n/\n  grant b;\n",
                unwrap(script, unit(script, "CREATE U1", "DATA1", "U1\n/\n")));
    }

    private static String unwrap(String script, WrappedUnit... units) {
        return new ScriptUnwrapper(script, List.of(units)).unwrap();
    }

    private static WrappedUnit unit(String script, String header, String data, String text) {
        return unit(script, header, data, text, false);
    }

    /** Unit whose WRAP drops the blanks at the start of a line after a {@code /} line, as for V2. */
    private static WrappedUnit droppingUnit(String script, String header, String data, String text) {
        return unit(script, header, data, text, true);
    }

    private static WrappedUnit unit(String script, String header, String data, String text, boolean dropsIndent) {
        int start = script.indexOf(header);
        return new FakeUnit(start, script.indexOf(data, start) + data.length(), text, dropsIndent);
    }

    /** Unit with the given bounds and a ready-made text. */
    private record FakeUnit(int start, int end, String text, boolean dropsIndentAfterSlash) implements WrappedUnit {

        @Override
        public String unitType() {
            return "PACKAGE";
        }

        @Override
        public String unitName() {
            return "P";
        }

        @Override
        public String plsqlVersion() {
            return null;
        }

        @Override
        public String plsqlCompatible() {
            return null;
        }

        @Override
        public Boolean staticSqlAsText() {
            return null;
        }

        @Override
        public String unwrap() {
            return text;
        }
    }
}
