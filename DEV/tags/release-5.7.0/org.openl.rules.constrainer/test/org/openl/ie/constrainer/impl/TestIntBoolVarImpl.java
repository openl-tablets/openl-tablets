package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntBoolVar;
import org.openl.ie.constrainer.impl.IntBoolExpAnd;
import org.openl.ie.constrainer.impl.IntBoolVarImpl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company: Exigen Group, Inc.
 * </p>
 *
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class TestIntBoolVarImpl extends TestCase {
    private Constrainer C = new Constrainer("TestIntBoolVarImpl");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntBoolVarImpl.class));
    }

    public TestIntBoolVarImpl(String name) {
        super(name);
    }

    public void testAll() {
        IntBoolVar boolvar1 = new IntBoolVarImpl(C, "intboolvar1"), boolvar2 = new IntBoolVarImpl(C, "intboolvar2");
        IntBoolExp boolexp = new IntBoolExpAnd(boolvar1, boolvar2);
        try {
            C.postConstraint(boolexp);
            assertEquals(1, boolvar1.value());
            assertEquals(1, boolvar2.value());
            try {
                boolvar1.setMax(0);
                fail("test failed");
            } catch (Failure f) {
            }
            try {
                boolvar2.setMax(0);
                fail("test failed");
            } catch (Failure f) {
            }
        } catch (Failure f) {
            fail("test failed!");
        }
    }
}