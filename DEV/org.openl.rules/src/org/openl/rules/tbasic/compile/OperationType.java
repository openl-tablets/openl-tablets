package org.openl.rules.tbasic.compile;

/**
 * Created by dl on 9/16/14.
 */
public enum OperationType {
    CHECK_LABEL("!CheckLabel"),
    COMPILE("!Compile"),
    DECLARE("!Declare"),
    DECLARE_ARRAY_ELEMENT("!DeclareArrayElement"),
    SUBROUTINE("!Subroutine"),
    FUNCTION("!Function");

    private final String name;

    OperationType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
