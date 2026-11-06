// SPDX-License-Identifier: AGPL-3.0-only

package com.github.sftwnd.unwrap;

import com.github.sftwnd.unwrap.spi.FormatUnwrapper;
import com.github.sftwnd.unwrap.spi.WrappedUnit;
import com.github.sftwnd.unwrap.v1.V1FormatUnwrapper;
import com.github.sftwnd.unwrap.v2.V2FormatUnwrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Restores PL/SQL source code from the Oracle wrapped format.
 * <p>
 * The format of each unit is detected by its header; the supported formats are listed in {@link WrapFormat}.
 * The object is immutable and thread-safe.
 */
public final class Unwrapper {

    /** Format implementations; a new format is added here and to {@link WrapFormat}. */
    private final List<FormatUnwrapper> formats;

    /** An unwrapper with {@link UnwrapOptions#defaults()}. */
    public Unwrapper() {
        this(UnwrapOptions.defaults());
    }

    /** An unwrapper with the given options. */
    public Unwrapper(UnwrapOptions options) {
        Objects.requireNonNull(options, "options");
        formats = List.of(new V1FormatUnwrapper(), new V2FormatUnwrapper(options.keepStrayCr()));
    }

    /** Whether the text contains a wrapped unit of a supported format; only the header is checked. */
    public boolean supports(String source) {
        Objects.requireNonNull(source, "source");
        return nearest(source, 0) != null;
    }

    /**
     * Unwraps the first wrapped unit of the text.
     *
     * @return the CREATE statement with the terminating {@code /} line
     * @throws UnwrapException if the text has no wrapped units or the first one cannot be unwrapped
     */
    public String unwrap(String source) {
        Objects.requireNonNull(source, "source");
        Candidate first = nearest(source, 0);
        if (first == null) {
            throw new UnwrapException("No wrapped unit found");
        }
        return first.reader().read(source, first.start()).unwrap();
    }

    /**
     * Unwraps all wrapped units of the text in text order.
     *
     * @throws UnwrapException if any unit cannot be parsed or unwrapped
     */
    public List<UnwrapResult> unwrapUnits(String source) {
        Objects.requireNonNull(source, "source");
        List<UnwrapResult> results = new ArrayList<>();
        for (Located located : units(source)) {
            WrappedUnit unit = located.unit();
            results.add(new UnwrapResult(located.format(), unit.plsqlVersion(), unit.plsqlCompatible(),
                    unit.staticSqlAsText(), unit.unitType(), unit.unitName(), unit.unwrap()));
        }
        return List.copyOf(results);
    }

    /**
     * Unwraps an SQL script: each wrapped unit together with its terminating {@code /} line is replaced with the
     * restored text; the rest of the text is copied unchanged, line endings included, and if the {@code /} line of the
     * last unit ends the script without a line break, the restored script ends the same way. After a V2 unit a
     * {@code --} line is written before each line that follows a {@code /} line and starts with blanks: after a PL/SQL
     * block WRAP 10.2 and later keep such blanks only after a comment line, which they remove.
     *
     * @throws UnwrapException if any unit cannot be parsed or unwrapped
     */
    public String unwrapScript(String script) {
        Objects.requireNonNull(script, "script");
        List<WrappedUnit> units = new ArrayList<>();
        for (Located located : units(script)) {
            units.add(located.unit());
        }
        return new ScriptUnwrapper(script, units).unwrap();
    }

    private List<Located> units(String text) {
        List<Located> units = new ArrayList<>();
        int position = 0;
        for (Candidate next = nearest(text, 0); next != null; next = nearest(text, position)) {
            WrappedUnit unit = next.reader().read(text, next.start());
            if (unit.end() <= next.start()) {
                throw new IllegalStateException("Wrapped unit of " + next.reader().format() + " has no content");
            }
            units.add(new Located(next.reader().format(), unit));
            position = unit.end();
        }
        return units;
    }

    /** Nearest unit header across all formats, or {@code null} if there are no more units. */
    private Candidate nearest(String text, int from) {
        Candidate nearest = null;
        for (FormatUnwrapper reader : formats) {
            int start = reader.find(text, from);
            if (start >= 0 && (nearest == null || start < nearest.start())) {
                nearest = new Candidate(reader, start);
            }
        }
        return nearest;
    }

    private record Candidate(FormatUnwrapper reader, int start) {
    }

    private record Located(WrapFormat format, WrappedUnit unit) {
    }
}
