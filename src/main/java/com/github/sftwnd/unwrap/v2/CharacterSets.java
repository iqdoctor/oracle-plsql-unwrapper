// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap.v2;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Character set of the text of a V2 unit. WRAP writes the ID of the client character set ({@code NLS_LANG}) into the
 * unit and leaves the text in it; without {@code NLS_LANG} the ID is US7ASCII, whatever the bytes are.
 */
final class CharacterSets {

    /** Oracle character set ID to the Java name; the IDs are those written by WRAP 10.2.0.5. */
    private static final Map<Integer, String> NAMES = Map.ofEntries(
            Map.entry(0x1, "US-ASCII"),         // US7ASCII
            Map.entry(0x4, "IBM437"),           // US8PC437
            Map.entry(0xa, "IBM850"),           // WE8PC850
            Map.entry(0x1f, "ISO-8859-1"),      // WE8ISO8859P1
            Map.entry(0x20, "ISO-8859-2"),      // EE8ISO8859P2
            Map.entry(0x23, "ISO-8859-5"),      // CL8ISO8859P5
            Map.entry(0x25, "ISO-8859-7"),      // EL8ISO8859P7
            Map.entry(0x2e, "ISO-8859-15"),     // WE8ISO8859P15
            Map.entry(0x33, "KOI8-U"),          // CL8KOI8U
            Map.entry(0x98, "IBM866"),          // RU8PC866
            Map.entry(0x9b, "IBM855"),          // RU8PC855
            Map.entry(0xaa, "windows-1250"),    // EE8MSWIN1250
            Map.entry(0xab, "windows-1251"),    // CL8MSWIN1251
            Map.entry(0xae, "windows-1253"),    // EL8MSWIN1253
            Map.entry(0xaf, "windows-1255"),    // IW8MSWIN1255
            Map.entry(0xb1, "windows-1254"),    // TR8MSWIN1254
            Map.entry(0xb2, "windows-1252"),    // WE8MSWIN1252
            Map.entry(0xb3, "windows-1257"),    // BLT8MSWIN1257
            Map.entry(0xc4, "KOI8-R"),          // CL8KOI8R
            Map.entry(0x230, "windows-1256"),   // AR8MSWIN1256
            Map.entry(0x340, "Shift_JIS"),      // JA16SJIS
            Map.entry(0x34e, "x-windows-949"),  // KO16MSWIN949
            Map.entry(0x354, "GBK"),            // ZHS16GBK
            Map.entry(0x361, "Big5"),           // ZHT16BIG5
            Map.entry(0x369, "UTF-8"));         // AL32UTF8

    private CharacterSets() {
    }

    /**
     * Text in the character set with the ID {@code id}. If the ID is unknown or the Java platform lacks the character
     * set, or the bytes are not valid in it, the text is UTF-8 if the bytes are valid UTF-8 and ISO-8859-1 otherwise,
     * which keeps every byte.
     */
    static String decode(byte[] bytes, int id) {
        Charset charset = charset(id);
        String text = charset == null ? null : strict(bytes, charset);
        if (text == null) {
            text = strict(bytes, StandardCharsets.UTF_8);
        }
        return text != null ? text : new String(bytes, StandardCharsets.ISO_8859_1);
    }

    /** Java character set of the ID, or {@code null} if the ID is unknown or the platform lacks it. */
    static Charset charset(int id) {
        String name = NAMES.get(id);
        return name != null && Charset.isSupported(name) ? Charset.forName(name) : null;
    }

    /** Decoded text, or {@code null} if the bytes are not valid in the character set. */
    private static String strict(byte[] bytes, Charset charset) {
        try {
            return charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }
}
