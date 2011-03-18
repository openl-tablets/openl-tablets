package org.openl.ie.constrainer;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author Sergej Vanskov
 * @version 1.0
 */

import org.openl.ie.constrainer.Constrainer;

import junit.framework.TestCase;

public class TestConstrainer extends TestCase {
    Constrainer C = new Constrainer("Testing constrainer");

    public static void main(String[] args) {
        // junit.textui.TestRunner.run (new TestSuite(TestFloatCalc.class));
    }

    public TestConstrainer(String name) {
        super(name);
    }

    public void testAddUndo() {

    }

}