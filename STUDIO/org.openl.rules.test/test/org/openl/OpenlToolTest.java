package org.openl;

import org.openl.binding.IBindingContext;
import org.openl.engine.OpenLManager;
import org.openl.meta.StringValue;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

import junit.framework.TestCase;

/*
 * Created on Mar 11, 2004
 *
 * Developed by OpenRules Inc. 2003-2004
 */

/**
 * @author snshor
 *
 */
public class OpenlToolTest extends TestCase {

    /**
     * Constructor for OpenlToolTest.
     *
     * @param name
     */
    public OpenlToolTest(String name) {
        super(name);
    }

    public void testMakeMethod() throws SyntaxNodeException {
        StringValue srcCode = new StringValue("5");

        OpenL openl = OpenL.getInstance(OpenL.OPENL_J_NAME);
        String name = "abc";
        IMethodSignature signature = IMethodSignature.VOID;
        IOpenClass declaringClass = null;

        IBindingContext cxt = openl.getBinder().makeBindingContext();

        IOpenMethod m = OpenLManager
            .makeMethodWithUnknownType(openl, srcCode.asSourceCodeModule(), name, signature, declaringClass, cxt);
        assertEquals(JavaOpenClass.INT, m.getType());

        srcCode = new StringValue("if (true) return 5.0; else return 9.1;");
        m = OpenLManager
            .makeMethodWithUnknownType(openl, srcCode.asSourceCodeModule(), name, signature, declaringClass, cxt);
        assertEquals(JavaOpenClass.DOUBLE, m.getType());

    }
}
