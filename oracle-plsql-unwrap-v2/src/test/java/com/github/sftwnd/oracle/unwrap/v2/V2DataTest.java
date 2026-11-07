// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap.v2;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V2DataTest {

    private static final byte[] TEXT = "PROCEDURE p IS BEGIN NULL; END;\0".getBytes(StandardCharsets.US_ASCII);

    @Test
    void substitutesBytesByPermutation() {
        int[] values = IntStream.range(0, V2Data.TABLE.length).map(i -> V2Data.TABLE[i] & 0xff).sorted().toArray();

        assertArrayEquals(IntStream.range(0, 256).toArray(), values);
    }

    @Test
    void readsTextWithoutTerminatingNul() {
        String data = V2Units.data(V2Units.deflate(TEXT));

        assertArrayEquals(Arrays.copyOf(TEXT, TEXT.length - 1), V2Data.text(data, TEXT.length));
    }

    @Test
    void readsTextLargerThan64Kilobytes() {
        byte[] text = ("PACKAGE BODY p IS\n" + String.join("", Collections.nCopies(5000, "  x := x + 1;\n")) + "END;\0")
                .getBytes(StandardCharsets.US_ASCII);
        assertTrue(text.length > 0x10000);

        assertEquals(text.length - 1, V2Data.text(V2Units.data(V2Units.deflate(text)), text.length).length);
    }

    @Test
    void rejectsDamagedData() {
        char[] data = V2Units.data(V2Units.deflate(TEXT)).toCharArray();
        data[30] = data[30] == 'A' ? 'B' : 'A';

        assertMessage("SHA-1 of the V2 unit data does not match", new String(data), TEXT.length);
    }

    @Test
    void rejectsTruncatedData() {
        String data = V2Units.data(V2Units.deflate(TEXT));

        assertMessage("SHA-1 of the V2 unit data does not match", data.substring(0, data.length() - 8), TEXT.length);
    }

    @Test
    void rejectsInvalidBase64() {
        assertMessage("Invalid Base64 data", "AAAAA", TEXT.length);
    }

    @Test
    void rejectsTooShortData() {
        assertMessage("V2 unit data is too short: 3 bytes", "AAAA", TEXT.length);
    }

    @Test
    void rejectsTextOfOtherLength() {
        String data = V2Units.data(V2Units.deflate(TEXT));

        assertMessage("Text of the V2 unit has 32 bytes, the unit gives 33", data, TEXT.length + 1);
        assertMessage("Text of the V2 unit is longer than the unit gives: 31 bytes", data, TEXT.length - 1);
    }

    @Test
    void rejectsTextWithoutNul() {
        byte[] text = Arrays.copyOf(TEXT, TEXT.length - 1);

        assertMessage("Text of the V2 unit does not end with NUL", V2Units.data(V2Units.deflate(text)), text.length);
    }

    @Test
    void rejectsDataAfterCompressedText() {
        byte[] stream = V2Units.deflate(TEXT);
        byte[] longer = Arrays.copyOf(stream, stream.length + 2);

        assertMessage("V2 unit data continues after the compressed text", V2Units.data(longer), TEXT.length);
    }

    @Test
    void rejectsTruncatedCompressedText() {
        byte[] stream = V2Units.deflate(TEXT);

        assertMessage("Compressed text of the V2 unit is truncated",
                V2Units.data(Arrays.copyOf(stream, stream.length - 6)), TEXT.length);
    }

    @Test
    void rejectsDamagedCompressedText() {
        byte[] stream = "not a zlib stream".getBytes(StandardCharsets.US_ASCII);

        assertMessage("Compressed text of the V2 unit is damaged", V2Units.data(stream), TEXT.length);
    }

    private static void assertMessage(String message, String data, int length) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> V2Data.text(data, length));
        assertTrue(e.getMessage().startsWith(message), e.getMessage());
    }
}
