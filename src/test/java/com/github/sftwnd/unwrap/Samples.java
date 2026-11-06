// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.ZipInputStream;

/**
 * Samples from {@code src/test/resources/samples}: the wrapped text ({@code .wrapped}) and the restored text
 * ({@code .unwrapped}). The synthetic samples ({@code P*}) are V2 scripts whose reference text is verified by wrapping
 * it again: WRAP of the same version gives the same unit. A sample is not stored in plain form: it is a Base64-encoded
 * zip archive with a single file. The content is read as ISO-8859-1 so that the comparison is byte-for-byte; a
 * reference of V2 units whose text is in other character sets is stored and read as UTF-8.
 */
public final class Samples {

    private Samples() {
    }

    /**
     * Reference output {@code reference} for the wrapped sample {@code wrapped}. The restored text takes the line break
     * of the wrapped unit; references are stored with CRLF, so for a sample wrapped with LF (WRAP for Linux) the line
     * breaks of the reference become LF. That is the text the round trip of such a sample is checked with.
     */
    public static String expected(String reference, String wrapped) {
        return expected(reference, wrapped, StandardCharsets.ISO_8859_1);
    }

    /** {@link #expected(String, String)} for a reference stored in {@code charset}. */
    public static String expected(String reference, String wrapped, Charset charset) {
        String text = read(reference, charset);
        return read(wrapped).contains("\r") ? text : text.replace("\r\n", "\n");
    }

    public static String read(String name) {
        return read(name, StandardCharsets.ISO_8859_1);
    }

    public static String read(String name, Charset charset) {
        try (InputStream in = Samples.class.getResourceAsStream("/samples/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("No sample " + name);
            }
            // The MIME decoder skips the Base64 line breaks
            byte[] archive = Base64.getMimeDecoder().decode(in.readAllBytes());
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
                if (zip.getNextEntry() == null) {
                    throw new IllegalArgumentException("Empty sample " + name);
                }
                return new String(zip.readAllBytes(), charset);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
