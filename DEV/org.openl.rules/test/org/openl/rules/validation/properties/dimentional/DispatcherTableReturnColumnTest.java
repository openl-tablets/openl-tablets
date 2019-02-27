package org.openl.rules.validation.properties.dimentional;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

public class DispatcherTableReturnColumnTest {

    @Test
    public void testGetparameterDeclaration() {
        DispatcherTableReturnColumn retColumn = createDTColumn(JavaOpenClass.FLOAT, null);
        assertEquals("float result", retColumn.getParameterDeclaration());

        retColumn = createDTColumn(NullOpenClass.the, null);
        assertEquals("null-Class result", retColumn.getParameterDeclaration());
    }

    @Test
    public void testGetCodeExpression() {
        DispatcherTableReturnColumn retColumn = createDTColumn(JavaOpenClass.FLOAT, null);
        assertEquals("result", retColumn.getCodeExpression());
    }

    @Test
    public void testGetTitle() {
        DispatcherTableReturnColumn retColumn = createDTColumn(JavaOpenClass.FLOAT, null);
        assertEquals("RESULT", retColumn.getTitle());
    }

    @Test
    public void testGetRuleValue() {
        IMethodSignature signature = new MethodSignature(new ParameterDeclaration(JavaOpenClass.STRING, "key"),
            new ParameterDeclaration(JavaOpenClass.FLOAT, "value"));
        DispatcherTableReturnColumn retColumn = createDTColumn(JavaOpenClass.FLOAT, signature);
        assertEquals("=aMethod$3(arg_key,arg_value)", retColumn.getRuleValue(3, 5));

        signature = IMethodSignature.VOID;
        retColumn = createDTColumn(JavaOpenClass.FLOAT, signature);
        assertEquals("=aMethod$7()", retColumn.getRuleValue(7, 9));
    }

    private DispatcherTableReturnColumn createDTColumn(IOpenClass type,
            IMethodSignature signature) {
        return new DispatcherTableReturnColumn(type, "aMethod", signature);
    }
}
