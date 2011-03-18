package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.ExpressionObserver;
import org.openl.ie.constrainer.impl.SubjectImpl;

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
 * @author Sergej Vanskov
 * @version 1.0
 */

public class TestSubjectImpl extends TestCase {
    private class SubjectImplFinal extends SubjectImpl {
        public SubjectImplFinal(Constrainer c) {
            super(c);
        }

        @Override
        public void propagate() {
        }
    }

    private class TestObserver extends ExpressionObserver {
        @Override
        public Object master() {
            return C;
        }
    }

    private static Constrainer C = new Constrainer("Test of SubjectImpl");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestSubjectImpl.class));
        IntVar intvar = C.addIntVar(-10, 10);
        // IntExp exp = new IntExpOpposite(intvar);
        IntExp exprrr = intvar.add(3);// new IntExpAddValue(intvar, 0);
        try {
            C.postConstraint(intvar.eq(exprrr));
        } catch (Failure f) {
        }
        IntExpArray array = new IntExpArray(C, intvar, exprrr);
        Goal goal = new GoalGenerate(array);
    }

    public TestSubjectImpl(String name) {
        super(name);
    }

    public void testAttachObserver() {

    };

}