package org.openl.binding.impl;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

public class MultiCallMethodBoundNodeTest {

    @Test
    public void testVoidType() {
        IOpenMethod method = Mockito.mock(IOpenMethod.class);
        IMethodCaller methodCaller = Mockito.mock(IMethodCaller.class);
        Mockito.when(methodCaller.getMethod()).thenReturn(method);
        Mockito.when(method.getType()).thenReturn(JavaOpenClass.VOID);

        MultiCallMethodBoundNode boundNode = new MultiCallMethodBoundNode(null,
            null,
            methodCaller,
            Collections.singletonList(2));

        assertEquals(JavaOpenClass.VOID, boundNode.getType());
    }

    @Test
    public void testArrayType() {
        IOpenMethod method = Mockito.mock(IOpenMethod.class);
        IMethodCaller methodCaller = Mockito.mock(IMethodCaller.class);
        Mockito.when(methodCaller.getMethod()).thenReturn(method);
        Mockito.when(method.getType()).thenReturn(JavaOpenClass.STRING);

        MultiCallMethodBoundNode boundNode = new MultiCallMethodBoundNode(null,
            null,
            methodCaller,
            Collections.singletonList(2));

        assertEquals(JavaOpenClass.STRING.getArrayType(1),
            boundNode.getType());
    }
}
