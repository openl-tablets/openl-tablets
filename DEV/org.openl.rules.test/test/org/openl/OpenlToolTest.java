package org.openl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.binding.IBindingContext;
import org.openl.engine.OpenLManager;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

/*
 * Created on Mar 11, 2004
 *
 * Developed by OpenRules Inc. 2003-2004
 */

/**
 * @author snshor
 */
public class OpenlToolTest {

    @Test
    public void testMakeMethod() {
        OpenL openl = OpenL.getInstance();
        String name = "abc";
        IMethodSignature signature = IMethodSignature.VOID;
        IOpenClass declaringClass = null;

        IBindingContext cxt = openl.getBinder().makeBindingContext();

        IOpenMethod m = OpenLManager
                .makeMethodWithUnknownType(openl, new StringSourceCodeModule("5", null), name, signature, declaringClass, cxt);
        assertEquals(JavaOpenClass.INT, m.getType());

        m = OpenLManager
                .makeMethodWithUnknownType(openl, new StringSourceCodeModule("if (true) return 5.0; else return 9.1;", null), name, signature, declaringClass, cxt);
        assertEquals(JavaOpenClass.DOUBLE, m.getType());

    }
}
