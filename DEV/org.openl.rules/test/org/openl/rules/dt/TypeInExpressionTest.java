package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.syntax.exception.SyntaxNodeException;

public class TypeInExpressionTest extends BaseOpenlBuilderHelper {

    public static final String SRC = "test/rules/dt/TypeInExpressionTest.xls";

    public TypeInExpressionTest() {
        super(SRC);
    }

    @Test
    public void test() {
        SyntaxNodeException[] exceptions = getCompiledOpenClass().getBindingErrors();
        assertTrue(exceptions.length == 1);
        assertEquals("Cannot execute expression with only type definition Double", exceptions[0].getMessage());
    }
}
