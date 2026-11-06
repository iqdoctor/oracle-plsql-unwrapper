// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap.v2;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterSetsTest {

    private static final String TEXT = "V := 'Привет'";

    /** The text in the character set of its ID. */
    @ParameterizedTest
    @CsvSource({"ab, windows-1251", "23, ISO-8859-5", "c4, KOI8-R", "369, UTF-8"})
    void readsTextInCharacterSetOfId(String id, String charset) {
        byte[] bytes = TEXT.getBytes(Charset.forName(charset));

        assertEquals(TEXT, CharacterSets.decode(bytes, Integer.parseInt(id, 16)));
    }

    /**
     * WRAP without {@code NLS_LANG} writes US7ASCII whatever the bytes are; an unknown ID does not tell them either, and
     * the bytes may be invalid in the character set of the ID. Valid UTF-8 is read as UTF-8, other bytes as ISO-8859-1.
     */
    @ParameterizedTest
    @CsvSource({"1, UTF-8, UTF-8", "1, ISO-8859-5, ISO-8859-1", "fff, UTF-8, UTF-8", "fff, windows-1251, ISO-8859-1",
            "369, windows-1251, ISO-8859-1"})
    void readsOtherBytesAsUtf8OrIso88591(String id, String written, String read) {
        byte[] bytes = TEXT.getBytes(Charset.forName(written));

        assertEquals(new String(bytes, Charset.forName(read)), CharacterSets.decode(bytes, Integer.parseInt(id, 16)));
    }

    @ParameterizedTest
    @CsvSource({"1, US-ASCII", "1f, ISO-8859-1", "b2, windows-1252", "340, Shift_JIS", "fff,"})
    void mapsIdsToJavaCharacterSets(String id, String charset) {
        Charset expected = charset == null ? null : Charset.forName(charset);

        assertEquals(expected, CharacterSets.charset(Integer.parseInt(id, 16)));
    }
}
