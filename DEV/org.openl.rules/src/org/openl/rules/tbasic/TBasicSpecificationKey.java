package org.openl.rules.tbasic;

/**
 * Created by dl on 9/16/14.
 */
public enum TBasicSpecificationKey {
    BREAK("BREAK"),
    CONTINUE("CONTINUE"),
    RETURN("RETURN"),
    FUNCTION("FUNCTION"),
    SUB("SUB"),
    END("END"),
    IF("IF"),
    ELSE("ELSE");

    private final String name;

    TBasicSpecificationKey(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
