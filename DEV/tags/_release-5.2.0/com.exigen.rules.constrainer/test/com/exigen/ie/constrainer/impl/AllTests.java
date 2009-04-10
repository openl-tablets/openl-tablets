package com.exigen.ie.constrainer.impl;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.TestConstrainer;
import com.exigen.ie.constrainer.TestConstrainerObjectImpl;
import com.exigen.ie.constrainer.TestFloatExpArray;
import com.exigen.ie.constrainer.TestGoalAnd;
import com.exigen.ie.constrainer.TestGoalDichotomize;
import com.exigen.ie.constrainer.TestGoalFail;
import com.exigen.ie.constrainer.TestGoalFastMinimize;
import com.exigen.ie.constrainer.TestGoalFloatMinimize;
import com.exigen.ie.constrainer.TestGoalGenerateAll;
import com.exigen.ie.constrainer.TestGoalImpl;
import com.exigen.ie.constrainer.TestGoalInstantiate;
import com.exigen.ie.constrainer.TestGoalMinimize;
import com.exigen.ie.constrainer.TestGoalOr;
import com.exigen.ie.constrainer.TestIntArray;
import com.exigen.ie.constrainer.TestIntArrayCards;
import com.exigen.ie.constrainer.TestIntBoolExpConst;
import com.exigen.ie.constrainer.TestIntExpConst;
import com.exigen.ie.constrainer.TestIntValueSelectors;
import com.exigen.ie.constrainer.TestIntVarSelectors;
import com.exigen.ie.constrainer.lpsolver.TestConstrainerLP;
import com.exigen.ie.constrainer.lpsolver.TestGoalSimplexSolve;
import com.exigen.ie.constrainer.lpsolver.impl.TestLPIntegerProblemImpl;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author Sergej Vanskov
 * @version 1.0
 */

public class AllTests extends TestSuite {

  public static Constrainer  C = new Constrainer ("Test");

  static final Class [] _testClasses = {
    AnotherFloatVarTest.class,
    TestIntCalc.class,
    TestFloatCalc.class,
    TestConstraintAllDiff.class,
    TestConstraintExpEqualsExp.class,
    TestConstraintExpEqualsValue.class,
    TestConstraintExpLessExp.class,
    TestConstraintExpLessValue.class,
    TestDomainBits.class,
    TestDomainBits2.class,
    TestDomainImpl.class,
    TestExecutionControl.class,
    TestExecutionControl2.class,
    TestExpressionFactoryImpl.class,
    TestFloatCalc.class,
    TestFloatDomainHistory.class,
    TestFloatDomainImpl.class,
    TestFloatExpAbs.class,
    TestFloatExpAddArray.class,
    TestFloatExpAddExp.class,
    TestFloatExpAddValue.class,
    TestFloatExpExponent.class,
    TestFloatExpIntExp.class,
    TestFloatExpInverse.class,
    TestFloatExpLog.class,
    TestFloatExpMulExp.class,
    TestFloatExpMultiplyPositive.class,
    TestFloatExpOpposite.class,
    TestFloatExpPowIntValue.class,
    TestFloatExpPowValue.class,
    TestFloatExpSqr.class,
    TestFloatVarImpl.class,
    TestGoalStack.class,
    TestIntBoolExpAnd.class,
    TestIntBoolExpEqExp.class,
    TestIntBoolExpEqValue.class,
    TestIntBoolExpFloatEqExp.class,
    TestIntBoolExpLessExp.class,
    TestIntBoolExpLessValue.class,
    TestIntBoolExpNot.class,
    TestIntBoolExpOr.class,
    TestIntBoolFloatLessExp.class,
    TestIntBoolVarImpl.class,
    TestIntCalc.class,
    TestIntDomainHistory.class,
    TestIntExpAbs.class,
    TestIntExpAddArray.class,
    TestIntExpAddArray1.class,
    TestIntExpAddExp.class,
    TestIntExpAddValue.class,
    TestIntExpArray.class,
    TestIntExpArrayElement.class,
    TestIntExpCard.class,
    TestIntExpCardIntExp.class,
    TestIntExpDivExp.class,
    TestIntExpElementAt.class,
    TestIntExpImpl.class,
    TestIntExpMultiplyPositive.class,
    TestIntExpOpposite.class,
    TestIntExpPositive.class,
    TestIntExpPowIntExp.class,
    TestIntExpPowIntValue.class,
    TestIntExpSqr.class,
    TestIntVarImpl.class,
    TestSubjectImpl.class,
    TestUndoableIntImpl.class,
    TestUndoStack.class,
    //TestUtils.class,
    TestLPIntegerProblemImpl.class,
    TestConstrainerLP.class,
    TestGoalSimplexSolve.class,
    TestConstrainer.class,
    TestConstrainerObjectImpl.class,
    TestFloatExpArray.class,
    TestGoalAnd.class,
    TestGoalDichotomize.class,
    TestGoalFail.class,
    TestGoalFastMinimize.class,
    TestGoalFloatMinimize.class,
    TestGoalGenerateAll.class,
    TestGoalImpl.class,
    TestGoalInstantiate.class,
    TestGoalMinimize.class,
    TestGoalOr.class,
    TestIntArray.class,
    TestIntArrayCards.class,
    TestIntBoolExpConst.class,
    TestIntExpConst.class,
    TestIntValueSelectors.class,
    TestIntVarSelectors.class
 };

  public static void main(String[] args) {
    TestRunner.run(suite());
  }

  public AllTests (String name) {
    super (name);
  }

  public static Test suite () {
    TestSuite suite = new TestSuite ();
    for(int i = 0; i < _testClasses.length; ++i)
      suite.addTestSuite (_testClasses [i]);
    return suite;
  }
}