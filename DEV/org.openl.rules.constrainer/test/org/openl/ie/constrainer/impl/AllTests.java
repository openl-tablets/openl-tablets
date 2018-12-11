package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.TestConstrainerObjectImpl;
import org.openl.ie.constrainer.TestFloatExpArray;
import org.openl.ie.constrainer.TestGoalAnd;
import org.openl.ie.constrainer.TestGoalDichotomize;
import org.openl.ie.constrainer.TestGoalFail;
import org.openl.ie.constrainer.TestGoalFastMinimize;
import org.openl.ie.constrainer.TestGoalFloatMinimize;
import org.openl.ie.constrainer.TestGoalGenerateAll;
import org.openl.ie.constrainer.TestGoalImpl;
import org.openl.ie.constrainer.TestGoalInstantiate;
import org.openl.ie.constrainer.TestGoalMinimize;
import org.openl.ie.constrainer.TestGoalOr;
import org.openl.ie.constrainer.TestIntArray;
import org.openl.ie.constrainer.TestIntArrayCards;
import org.openl.ie.constrainer.TestIntBoolExpConst;
import org.openl.ie.constrainer.TestIntExpConst;
import org.openl.ie.constrainer.TestIntValueSelectors;
import org.openl.ie.constrainer.TestIntVarSelectors;
import org.openl.ie.constrainer.lpsolver.TestConstrainerLP;
import org.openl.ie.constrainer.lpsolver.TestGoalSimplexSolve;
import org.openl.ie.constrainer.lpsolver.impl.TestLPIntegerProblemImpl;

import junit.framework.Test;
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

public class AllTests extends TestSuite {

    public static Constrainer C = new Constrainer("Test");

    static final Class[] _testClasses = { AnotherFloatVarTest.class, TestIntCalc.class, TestFloatCalc.class,
            TestConstraintAllDiff.class, TestConstraintExpEqualsExp.class, TestConstraintExpEqualsValue.class,
            TestConstraintExpLessExp.class, TestConstraintExpLessValue.class, TestDomainBits.class,
            TestDomainBits2.class, TestDomainImpl.class, TestExecutionControl.class, TestExecutionControl2.class,
            TestExpressionFactoryImpl.class, TestFloatCalc.class, TestFloatDomainHistory.class,
            TestFloatDomainImpl.class, TestFloatExpAbs.class, TestFloatExpAddArray.class, TestFloatExpAddExp.class,
            TestFloatExpAddValue.class, TestFloatExpExponent.class, TestFloatExpIntExp.class,
            TestFloatExpInverse.class, TestFloatExpLog.class, TestFloatExpMulExp.class,
            TestFloatExpMultiplyPositive.class, TestFloatExpOpposite.class, TestFloatExpPowIntValue.class,
            TestFloatExpPowValue.class, TestFloatExpSqr.class, TestFloatVarImpl.class, TestGoalStack.class,
            TestIntBoolExpAnd.class, TestIntBoolExpEqExp.class, TestIntBoolExpEqValue.class,
            TestIntBoolExpFloatEqExp.class, TestIntBoolExpLessExp.class, TestIntBoolExpLessValue.class,
            TestIntBoolExpNot.class, TestIntBoolExpOr.class, TestIntBoolFloatLessExp.class, TestIntBoolVarImpl.class,
            TestIntCalc.class, TestIntDomainHistory.class, TestIntExpAbs.class, TestIntExpAddArray.class,
            TestIntExpAddArray1.class, TestIntExpAddExp.class, TestIntExpAddValue.class, TestIntExpArray.class,
            TestIntExpArrayElement.class, TestIntExpCard.class, TestIntExpCardIntExp.class, TestIntExpDivExp.class,
            TestIntExpElementAt.class, TestIntExpImpl.class, TestIntExpMultiplyPositive.class,
            TestIntExpOpposite.class, TestIntExpPositive.class, TestIntExpPowIntExp.class,
            TestIntExpPowIntValue.class,
            TestIntExpSqr.class,
            TestIntVarImpl.class,
            TestSubjectImpl.class,
            TestUndoableIntImpl.class,
            TestUndoStack.class,
            // TestUtils.class,
            TestLPIntegerProblemImpl.class, TestConstrainerLP.class, TestGoalSimplexSolve.class,
            TestConstrainerObjectImpl.class, TestFloatExpArray.class, TestGoalAnd.class, TestGoalDichotomize.class,
            TestGoalFail.class, TestGoalFastMinimize.class, TestGoalFloatMinimize.class, TestGoalGenerateAll.class,
            TestGoalImpl.class, TestGoalInstantiate.class, TestGoalMinimize.class, TestGoalOr.class,
            TestIntArray.class, TestIntArrayCards.class, TestIntBoolExpConst.class, TestIntExpConst.class,
            TestIntValueSelectors.class, TestIntVarSelectors.class };

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        for (int i = 0; i < _testClasses.length; ++i) {
            suite.addTestSuite(_testClasses[i]);
        }
        return suite;
    }

    public AllTests(String name) {
        super(name);
    }
}