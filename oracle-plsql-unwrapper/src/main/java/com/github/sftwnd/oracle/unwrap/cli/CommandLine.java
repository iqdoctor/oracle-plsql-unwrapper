// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap.cli;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

/** Parsed arguments of the command line; a wrong one is reported as {@link IllegalArgumentException}. */
final class CommandLine {

    private static final String STANDARD_STREAM = "-";

    private boolean help;
    private boolean keepStrayCr;
    private Charset inputEncoding = StandardCharsets.ISO_8859_1;
    private Charset outputEncoding;
    private String input = STANDARD_STREAM;
    private String output = STANDARD_STREAM;

    private CommandLine() {
    }

    static CommandLine parse(String... args) {
        CommandLine line = new CommandLine();
        int files = 0;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-h".equals(arg) || "--help".equals(arg)) {
                line.help = true;
            } else if ("--keep-stray-cr".equals(arg)) {
                line.keepStrayCr = true;
            } else if ("--input-encoding".equals(arg)) {
                line.inputEncoding = charset(arg, args, ++i);
            } else if ("--output-encoding".equals(arg)) {
                line.outputEncoding = charset(arg, args, ++i);
            } else if (arg.length() > 1 && arg.charAt(0) == '-') {
                throw new IllegalArgumentException("Unknown option " + arg);
            } else if (files == 0) {
                line.input = arg;
                files++;
            } else if (files == 1) {
                line.output = arg;
                files++;
            } else {
                throw new IllegalArgumentException("Unexpected argument " + arg);
            }
        }
        return line;
    }

    private static Charset charset(String option, String[] args, int index) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Option " + option + " needs an encoding");
        }
        try {
            return Charset.forName(args[index]);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new IllegalArgumentException("Unknown encoding " + args[index], e);
        }
    }

    boolean help() {
        return help;
    }

    boolean keepStrayCr() {
        return keepStrayCr;
    }

    Charset inputEncoding() {
        return inputEncoding;
    }

    /** The encoding of the result: the one of the input unless it is given. */
    Charset outputEncoding() {
        return outputEncoding != null ? outputEncoding : inputEncoding;
    }

    /** The input file, or {@code null} for the standard input. */
    String input() {
        return STANDARD_STREAM.equals(input) ? null : input;
    }

    /** The output file, or {@code null} for the standard output. */
    String output() {
        return STANDARD_STREAM.equals(output) ? null : output;
    }
}
