// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap.v2;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.zip.Deflater;

/** Builds V2 units in the layout of WRAP 10.2.0.5, for cases WRAP itself does not produce. */
final class V2Units {

    private static final int LINE_LENGTH = 72;

    private V2Units() {
    }

    /** Unit with the text in {@code charset} followed by NUL, as WRAP writes it. */
    static String unit(String header, int characterSet, String kind, String text, Charset charset) {
        byte[] bytes = text.getBytes(charset);
        byte[] content = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, content, 0, bytes.length);
        return unit(header, characterSet, kind, content.length, data(deflate(content)));
    }

    /** Unit with the given text length and Base64 data. */
    static String unit(String header, int characterSet, String kind, int length, String data) {
        StringBuilder unit = new StringBuilder(header).append(" wrapped \na000000\n")
                .append(Integer.toHexString(characterSet)).append('\n');
        unit.append(String.join("", Collections.nCopies(15, "abcd\n"))).append(kind).append('\n');
        int chars = data.length();
        unit.append(Integer.toHexString(length)).append(' ').append(Integer.toHexString(chars + chars / LINE_LENGTH))
                .append('\n');
        for (int i = 0; i < chars; i += LINE_LENGTH) {
            unit.append(data, i, Math.min(chars, i + LINE_LENGTH)).append('\n');
        }
        return unit.append("\n/\n").toString();
    }

    /** Base64 data of a stream: its SHA-1 digest and the stream, every byte substituted as WRAP does. */
    static String data(byte[] stream) {
        byte[] digest = sha1(stream);
        byte[] data = new byte[digest.length + stream.length];
        System.arraycopy(digest, 0, data, 0, digest.length);
        System.arraycopy(stream, 0, data, digest.length, stream.length);
        byte[] reverse = new byte[256];
        for (int i = 0; i < 256; i++) {
            reverse[V2Data.TABLE[i] & 0xff] = (byte) i;
        }
        for (int i = 0; i < data.length; i++) {
            data[i] = reverse[data[i] & 0xff];
        }
        return Base64.getEncoder().encodeToString(data);
    }

    /** Zlib stream of the content at level 9, as WRAP writes it. */
    static byte[] deflate(byte[] content) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(content);
            deflater.finish();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                stream.write(buffer, 0, deflater.deflate(buffer));
            }
            return stream.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] sha1(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
