// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap.cli;

import com.github.sftwnd.oracle.unwrap.Unwrapper;
import com.github.sftwnd.oracle.unwrap.spi.UnwrapException;
import com.github.sftwnd.oracle.unwrap.spi.UnwrapOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Command line of the unwrapper: restores an SQL script from a file or the standard input. The help lists the formats
 * found through the SPI, so it tells what this distribution restores. The bytes are read as they are; the default
 * encoding is single-byte, so the text outside the units comes out byte for byte.
 */
public final class UnwrapperCli {

    private static final String COPYRIGHT = "Copyright (c) 2026, Andrey D. Shindarev";

    private static final int SUCCESS = 0;
    private static final int FAILURE = 1;
    private static final int WRONG_USAGE = 2;

    private final InputStream in;
    private final OutputStream out;
    private final PrintStream err;

    UnwrapperCli(InputStream in, OutputStream out, PrintStream err) {
        this.in = in;
        this.out = out;
        this.err = err;
    }

    public static void main(String[] args) {
        System.exit(new UnwrapperCli(System.in, System.out, System.err).run(args));
    }

    int run(String... args) {
        CommandLine line;
        try {
            line = CommandLine.parse(args);
        } catch (IllegalArgumentException e) {
            err.println(e.getMessage());
            err.println("Try --help");
            return WRONG_USAGE;
        }
        try {
            Unwrapper unwrapper = new Unwrapper(UnwrapOptions.defaults().withKeepStrayCr(line.keepStrayCr()));
            if (line.help()) {
                write(help(unwrapper).getBytes(Charset.defaultCharset()), null);
            } else {
                String script = new String(read(line.input()), line.inputEncoding());
                write(encode(unwrapper.unwrapScript(script), line.outputEncoding()), line.output());
            }
            return SUCCESS;
        } catch (UnwrapException | IllegalStateException e) {
            err.println(e.getMessage());
            return FAILURE;
        } catch (IOException e) {
            err.println(e);
            return FAILURE;
        }
    }

    private static String help(Unwrapper unwrapper) {
        StringBuilder help = new StringBuilder();
        help.append("Usage: unwrapper [options] [input [output]]\n")
                .append('\n')
                .append("Restores the PL/SQL source of an SQL script: each wrapped unit is replaced with its text,\n")
                .append("the rest of the script is copied. Without files, or with -, the standard input and output\n")
                .append("are used.\n")
                .append('\n')
                .append("This unwrapper restores only the wrapped formats:\n");
        unwrapper.formats().forEach((format, description) ->
                help.append("  ").append(format).append("  ").append(description).append('\n'));
        return help.append('\n')
                .append("Options:\n")
                .append("  --input-encoding <charset>   encoding of the input, ISO-8859-1 by default\n")
                .append("  --output-encoding <charset>  encoding of the output, the one of the input by default\n")
                .append("  --keep-stray-cr              keep a stray CR of a V2 text instead of a space\n")
                .append("  -h, --help                   print this help\n")
                .append('\n')
                .append(COPYRIGHT).append('\n')
                .toString();
    }

    private byte[] read(String file) throws IOException {
        if (file != null) {
            return Files.readAllBytes(Paths.get(file));
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int count = in.read(buffer); count >= 0; count = in.read(buffer)) {
            bytes.write(buffer, 0, count);
        }
        return bytes.toByteArray();
    }

    private void write(byte[] bytes, String file) throws IOException {
        if (file != null) {
            Files.write(Paths.get(file), bytes);
        } else {
            out.write(bytes);
            out.flush();
        }
    }

    /** A character the encoding cannot hold is an error: a replacement would change the restored text. */
    private static byte[] encode(String text, Charset charset) {
        try {
            ByteBuffer bytes = charset.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(text));
            byte[] result = new byte[bytes.remaining()];
            bytes.get(result);
            return result;
        } catch (CharacterCodingException e) {
            throw new UnwrapException("The restored text does not fit into " + charset.name()
                    + ": choose another --output-encoding, for example UTF-8", e);
        }
    }
}
