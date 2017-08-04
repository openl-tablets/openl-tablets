package org.openl.rules.tbasic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

public class NotVisibleVariableTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/tbasic1/NOT_VISIBLE_VAR.xlsx";
    
    public NotVisibleVariableTest() {
        super(SRC);
    }
    
    @Test
    public void test() {
        assertEquals(4, getCompiledOpenClass().getBindingErrors().length);
        assertEquals("Field not found: 'premium'", getCompiledOpenClass().getBindingErrors()[0].getMessage());
        assertEquals("Field not found: 'premium'", getCompiledOpenClass().getBindingErrors()[1].getMessage());
        assertEquals("Field not found: 'premium'", getCompiledOpenClass().getBindingErrors()[2].getMessage());
        assertEquals("Field not found: 'premium'", getCompiledOpenClass().getBindingErrors()[3].getMessage());
        assertEquals(0, getCompiledOpenClass().getParsingErrors().length);
    }
}
 
