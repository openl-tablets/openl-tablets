package org.openl.meta.number;

/**
 * Operations with descriptions for {@link org.openl.meta.explanation.ExplanationNumberValue}.<br>
 * Including formulas and functions.
 *
 * @author DLiauchuk
 *
 */
public enum NumberOperations {
    COPY("COPY"),
    MAX("max"),
    MAX_IN_ARRAY("max"),
    MIN("min"),
    MIN_IN_ARRAY("min"),
    ROUND("round"),
    POW("pow"),
    ABS("abs"),
    AVG("average"),
    SUM("sum"),
    MEDIAN("median"),
    PRODUCT("product"),
    QUOTIENT("quotient"),
    MOD("mod"),
    NEGATIVE("negative"),
    INC("inc"),
    POSITIVE("positive"),
    DEC("dec"),
    SMALL("small"),
    BIG("big");

    private String description;

    private NumberOperations(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    public String getName() {
        return name().toLowerCase();
    }
}
