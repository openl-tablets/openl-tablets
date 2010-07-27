package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.impl.IntExpPositive;

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

public class TestIntExpPositive extends TestCase {
    private Constrainer C = new Constrainer("TestIntExpPositive");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpPositive.class));
    }

    public TestIntExpPositive(String name) {
        super(name);
    }

    public void testA() {
        IntExpPositive exp = new IntExpPositive(C.addIntVar(-10, 10));
    }
}