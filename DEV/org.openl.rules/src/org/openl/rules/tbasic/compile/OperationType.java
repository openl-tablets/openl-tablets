package org.openl.rules.tbasic.compile;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Created by dl on 9/16/14.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum OperationType {
    CHECK_LABEL("!CheckLabel"),
    COMPILE("!Compile"),
    DECLARE("!Declare"),
    DECLARE_ARRAY_ELEMENT("!DeclareArrayElement"),
    SUBROUTINE("!Subroutine"),
    FUNCTION("!Function");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
