package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntBoolExpConst;

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

public class TestIntBoolExpConst extends TestCase {
    static private Constrainer C = new Constrainer("test IntBoolExpConst");
    static private IntBoolExpConst bconst = new IntBoolExpConst(C, true);

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntBoolExpConst.class));
    }

    public TestIntBoolExpConst(String name) {
        super(name);
    }

    public void testAnd() {
        assertTrue((bconst.and(true)).isTrue());
        assertTrue(bconst.and(false).isFalse());
        assertTrue(bconst.and(new IntBoolExpConst(C, true)).isTrue());
        assertTrue(bconst.and(new IntBoolExpConst(C, false)).isFalse());
    }

    public void testAsConstraint() {
        try {
            bconst.asConstraint().execute();
        } catch (Failure f) {
            fail("BoolIntExpConst.testAsConstraint doesn't work properly: " + f);
        } catch (Throwable ex) {
            fail("Unexpected exception: " + ex);
        }
        try {
            bconst.and(false).asConstraint().execute();
            fail("BoolIntExpConst.testAsConstraint doesn't work properly: ");
        } catch (Failure f) {
        } catch (Throwable ex) {
            fail("Unexpected exception: " + ex);
        }
    }

    public void testGetIntBoolExpConst() {
        assertTrue(IntBoolExpConst.getIntBoolExpConst(C, true).isTrue());
        assertTrue(IntBoolExpConst.getIntBoolExpConst(C, false).isFalse());
    }

    public void testImplies() {
        assertTrue(bconst.implies(true).isTrue());
        assertTrue(bconst.implies(false).isFalse());
        assertTrue(bconst.implies(new IntBoolExpConst(C, true)).isTrue());
        assertTrue(bconst.implies(new IntBoolExpConst(C, false)).isFalse());
    }

    public void testNot() {
        assertTrue(bconst.not().isFalse());
        assertTrue(IntBoolExpConst.getIntBoolExpConst(C, false).not().isTrue());
    }

    public void testOr() {
        assertTrue(bconst.or(true).isTrue());
        assertTrue(bconst.or(false).isTrue());
        assertTrue(bconst.not().or(true).isTrue());
        assertTrue(bconst.not().or(false).isFalse());

        IntBoolExpConst falseConst = IntBoolExpConst.getIntBoolExpConst(C, false);
        IntBoolExpConst trueConst = IntBoolExpConst.getIntBoolExpConst(C, true);

        assertTrue(bconst.or(trueConst).isTrue());
        assertTrue(bconst.or(falseConst).isTrue());
        assertTrue(bconst.not().or(trueConst).isTrue());
        assertTrue(bconst.not().or(falseConst).isFalse());
    }

}