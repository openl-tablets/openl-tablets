package com.exigen.ie.constrainer.impl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestExpressionFactoryImpl extends TestCase {

  public TestExpressionFactoryImpl(String name) {super(name);}
  public static void main(String[] args) {
    TestRunner.run(new TestSuite(TestExpressionFactoryImpl.class));
  }

  public void testVoid(){}

}