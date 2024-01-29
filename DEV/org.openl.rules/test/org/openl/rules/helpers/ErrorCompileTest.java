package org.openl.rules.helpers;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;

public class ErrorCompileTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/ErrorCompileTest.xlsx";

    public ErrorCompileTest() {
        super(SRC);
    }

    @Test
    public void test() {
        assertFalse(getCompiledOpenClass().hasErrors());
    }

}
