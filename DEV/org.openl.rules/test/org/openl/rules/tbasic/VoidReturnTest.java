package org.openl.rules.tbasic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

public class VoidReturnTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/tbasic0/VoidReturn.xls";

    public VoidReturnTest() {
        super(SRC);
    }

    @Test
    public void test() {
        assertEquals(1, getCompiledOpenClass().getBindingErrors().length);
        assertEquals("Cannot convert from 'void' to 'int'.", getCompiledOpenClass().getBindingErrors()[0].getMessage());
        assertTrue(getCompiledOpenClass().getParsingErrors().length == 0);
    }
}
