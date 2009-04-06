package com.exigen.ie.constrainer;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestGoalImpl extends TestCase {

  public TestGoalImpl(String name) {super(name);}
  public static void main(String[] args) {
    TestRunner.run(new TestSuite(TestGoalImpl.class));
  }

  public void testExecute(){}
}