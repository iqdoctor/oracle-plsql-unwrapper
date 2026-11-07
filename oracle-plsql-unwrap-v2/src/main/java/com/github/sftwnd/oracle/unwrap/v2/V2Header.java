// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap.v2;

import java.util.Objects;

/**
 * Header of a V2 unit. WRAP copies the CREATE statement from the source up to the end of the unit name, or up to the
 * end of the last of the SHARING, TIMESTAMP and OID clauses after it, and appends {@code " wrapped "}; text taken from
 * the data dictionary starts with the kind of the unit instead of CREATE. WRAP 10.1 also copies the character after the
 * last clause, and in the text of the unit it replaces the clauses and that character with spaces.
 */
final class V2Header {

    private final String unitType;
    private final String unitName;
    private final String simpleName;
    private final String text;
    private final String clauseEnd;
    private final int clausesLength;
    private final boolean create;

    /**
     * @param unitType      unit type, for example {@code PACKAGE BODY}
     * @param unitName      unit name with the schema if one is given; a quoted part without its quotes, others in upper
     *                      case
     * @param simpleName    the name without the schema, written as in {@code unitName}
     * @param text          the header as WRAP copied it, with the blanks before it on its line: case, spacing, schema,
     *                      quotes, line breaks, {@code IF NOT EXISTS} and the clauses are kept
     * @param clauseEnd     the character WRAP 10.1 copied after the last clause, empty in other headers
     * @param clausesLength length of the part of the text after the unit name that ends with the spaces of WRAP 10.1 in
     *                      place of the clauses, 0 in other headers
     * @param create        whether the header starts with CREATE
     */
    V2Header(String unitType, String unitName, String simpleName, String text, String clauseEnd, int clausesLength,
            boolean create) {
        Objects.requireNonNull(unitType, "unitType");
        Objects.requireNonNull(unitName, "unitName");
        Objects.requireNonNull(simpleName, "simpleName");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(clauseEnd, "clauseEnd");
        this.unitType = unitType;
        this.unitName = unitName;
        this.simpleName = simpleName;
        this.text = text;
        this.clauseEnd = clauseEnd;
        this.clausesLength = clausesLength;
        this.create = create;
    }

    String unitType() {
        return unitType;
    }

    String unitName() {
        return unitName;
    }

    String simpleName() {
        return simpleName;
    }

    String text() {
        return text;
    }

    String clauseEnd() {
        return clauseEnd;
    }

    int clausesLength() {
        return clausesLength;
    }

    boolean create() {
        return create;
    }
}
