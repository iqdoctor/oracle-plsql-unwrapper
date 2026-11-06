// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap.v2;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Data of a V2 unit: Base64 of a byte substitution applied to the SHA-1 digest of a zlib stream followed by the stream.
 * The stream holds the text of the unit and a terminating NUL.
 */
final class V2Data {

    private static final int DIGEST_LENGTH = 20;

    /**
     * Byte substitution of the data: the data byte {@code b} stands for {@code TABLE[b]}. The table is a permutation
     * of 0..255 fixed in the Oracle libraries; it applies to every byte of the data, the digest included.
     */
    static final byte[] TABLE = HexFormat.of().parseHex("3d6585b318dbe287f152ab634bb5a05f"
            + "7d687b9b24c228678adea4261e03eb17"
            + "6f343e7a3fd2a96a0fe935561fb14d10"
            + "78d975f6bc4104816106f9add6d5297e"
            + "869e79e505ba84cc6e278eb05da8f39f"
            + "d0a271b858dd2c38994c480755e4538c"
            + "46b62da5af322240dc50c3a1258b9c16"
            + "605ccffd0c981cd4376d3c3a30e86c31"
            + "47f533da43c8e35e1994ece6a39514e0"
            + "9d64fa5915c52fcabb0bdff297bf0a76"
            + "b449445a1df0009621807f1a82394fc1"
            + "a7d70dd1d8ff139370ee5befbe09b977"
            + "72e7b254b72ac7739066200e51edf87c"
            + "8f2ef412c62b83cdaccb3bc44ec06936"
            + "6202ae88fcaa4208a64557d39abde123"
            + "8d924a1189746b91fbfec901ea1bf7ce");

    private V2Data() {
    }

    /**
     * Text bytes of the unit without the terminating NUL.
     *
     * @param base64 the Base64 lines without line breaks and blanks
     * @param length the text length from the unit, the NUL included
     * @throws IllegalArgumentException if the data is damaged or truncated, or does not match the length
     */
    static byte[] text(String base64, int length) {
        byte[] data;
        try {
            data = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 data of the V2 unit", e);
        }
        if (data.length <= DIGEST_LENGTH) {
            throw new IllegalArgumentException("V2 unit data is too short: " + data.length + " bytes");
        }
        for (int i = 0; i < data.length; i++) {
            data[i] = TABLE[data[i] & 0xff];
        }
        byte[] digest = sha1().digest(Arrays.copyOfRange(data, DIGEST_LENGTH, data.length));
        if (!MessageDigest.isEqual(digest, Arrays.copyOf(data, DIGEST_LENGTH))) {
            throw new IllegalArgumentException("SHA-1 of the V2 unit data does not match: the data is damaged or"
                    + " truncated");
        }
        byte[] text = inflate(data, length);
        if (text.length == 0 || text[text.length - 1] != 0) {
            throw new IllegalArgumentException("Text of the V2 unit does not end with NUL");
        }
        if (text.length != length) {
            throw new IllegalArgumentException("Text of the V2 unit has " + text.length + " bytes, the unit gives "
                    + length);
        }
        return Arrays.copyOf(text, text.length - 1);
    }

    /** Inflates the zlib stream after the digest; a text longer than {@code length} is not read to the end. */
    private static byte[] inflate(byte[] data, int length) {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(data, DIGEST_LENGTH, data.length - DIGEST_LENGTH);
            ByteArrayOutputStream text = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && !inflater.finished()) {
                    throw new IllegalArgumentException("Compressed text of the V2 unit is truncated");
                }
                text.write(buffer, 0, count);
                if (text.size() > length) {
                    throw new IllegalArgumentException("Text of the V2 unit is longer than the unit gives: "
                            + length + " bytes");
                }
            }
            if (inflater.getRemaining() > 0) {
                throw new IllegalArgumentException("V2 unit data continues after the compressed text");
            }
            return text.toByteArray();
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("Compressed text of the V2 unit is damaged", e);
        } finally {
            inflater.end();
        }
    }

    private static MessageDigest sha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform implements SHA-1
            throw new IllegalStateException(e);
        }
    }
}
