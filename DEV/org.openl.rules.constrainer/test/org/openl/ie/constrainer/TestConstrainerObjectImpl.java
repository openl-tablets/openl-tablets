package org.openl.ie.constrainer;

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
import junit.textui.TestRunner;

public class TestConstrainerObjectImpl extends TestCase {

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestConstrainerObjectImpl.class));
    }

    public TestConstrainerObjectImpl(String name) {
        super(name);
    }

    public void testVoid() {
    }
}