/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package all.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.openl.types.java.JavaOpenClassTest;
import org.openl.util.OpenIteratorTest;
import org.openl.util.TreeIteratorTest;
import org.openl.util.text.TextIntervalTest;

/**
 * @author snshor
 *
 */
public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for openl");
        // $JUnit-BEGIN$
        suite.addTest(new TestSuite(OpenIteratorTest.class));
        suite.addTest(new TestSuite(TreeIteratorTest.class));
        suite.addTest(new TestSuite(TextIntervalTest.class));

        suite.addTest(new TestSuite(JavaOpenClassTest.class));

        // $JUnit-END$
        return suite;
    }
}
