package com.exigen.ie.constrainer;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author Sergej Vanskov
 * @version 1.0
 */
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestConstrainerObjectImpl extends TestCase{

  public TestConstrainerObjectImpl(String name) {super(name); }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(new TestSuite(TestConstrainerObjectImpl.class));
  }

  public void testVoid(){}
}