package org.openl.rules.tbasic;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Created by dl on 9/16/14.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
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

    @Override
    public String toString() {
        return name;
    }
}
