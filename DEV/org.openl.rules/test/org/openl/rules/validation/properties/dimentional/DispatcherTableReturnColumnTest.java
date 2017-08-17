package org.openl.rules.validation.properties.dimentional;

import org.junit.Test;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.MethodSignature;
import org.openl.types.java.JavaOpenClass;

import static org.junit.Assert.assertEquals;

public class DispatcherTableReturnColumnTest {

    @Test
    public void testGetparameterDeclaration() {
        IDecisionTableColumn retColumn = createDTColumn(JavaOpenClass.FLOAT, null);
        assertEquals("float result", retColumn.getParameterDeclaration());

        retColumn = createDTColumn(NullOpenClass.the, null);
        assertEquals("null-Class result", retColumn.getParameterDeclaration());
    }

    @Test
    public void testGetCodeExpression() {
        IDecisionTableColumn retColumn = createDTColumn(JavaOpenClass.FLOAT, null);
        assertEquals("result", retColumn.getCodeExpression());
    }

    @Test
    public void testGetTitle() {
        IDecisionTableColumn retColumn = createDTColumn(JavaOpenClass.FLOAT, null);
        assertEquals("RESULT", retColumn.getTitle());
    }

    @Test
    public void testGetRuleValue() {
        MethodSignature signature = new MethodSignature(new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.FLOAT }, new String[] {"key", "value"});
        IDecisionTableReturnColumn retColumn = createDTColumn(JavaOpenClass.FLOAT, signature);
        assertEquals("=aMethod$3(arg_key,arg_value)", retColumn.getRuleValue(3, 5));

        signature = new MethodSignature(new IOpenClass[0], new String[0]);
        retColumn = createDTColumn(JavaOpenClass.FLOAT, signature);
        assertEquals("=aMethod$7()", retColumn.getRuleValue(7, 9));
    }

    private DispatcherTableReturnColumn createDTColumn(IOpenClass type,
            IMethodSignature signature) {
        return new DispatcherTableReturnColumn(type, "aMethod", signature);
    }
}
