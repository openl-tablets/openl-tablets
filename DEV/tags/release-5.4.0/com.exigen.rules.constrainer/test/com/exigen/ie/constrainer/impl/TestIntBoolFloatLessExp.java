package com.exigen.ie.constrainer.impl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatExpArray;
import com.exigen.ie.constrainer.FloatExpConst;
import com.exigen.ie.constrainer.GoalFloatGenerate;

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

public class TestIntBoolFloatLessExp extends TestCase {
    private Constrainer C = new Constrainer("TestIntBoolFloatLessExp");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntBoolFloatLessExp.class));
    }

    public TestIntBoolFloatLessExp(String name) {
        super(name);
    }

    public void testIntBoolFloatLessExp() {
        FloatExpArray array = new FloatExpArray(C, 10);
        double delta = Constrainer.precision();
        for (int i = 0; i < array.size(); i++) {
            array.set(C.addFloatVar(0, 9, ""), i);
        }

        IntBoolExpFloatLessExp[] boolexp = new IntBoolExpFloatLessExp[9];
        try {
            for (int i = 1; i < array.size(); i++) {
                boolexp[i - 1] = new IntBoolExpFloatLessExp(new FloatExpConst(C, 1.0), new FloatExpAddExp(
                        new FloatExpOpposite(array.get(i - 1)), array.get(i)));
                C.postConstraint(boolexp[i - 1]);
            }
        } catch (Failure f) {
            fail("test failed");
        }

        boolean flag = C.execute(new GoalFloatGenerate(array));
        assertTrue(flag);
        for (int i = 1; i < array.size(); i++) {
            try {
                assertTrue((array.get(i).value() - array.get(i - 1).value()) >= 1);
                assertEquals(i, array.get(i).value(), Constrainer.precision());
            } catch (Failure f) {
                fail("test failed due to incorrect work of FloatVar.value()");
            }
        }
    }

}