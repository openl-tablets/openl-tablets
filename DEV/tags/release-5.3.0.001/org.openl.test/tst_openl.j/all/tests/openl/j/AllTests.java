/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package all.tests.openl.j;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.openl.OpenlToolTest;
import org.openl.binding.BinderTest;
import org.openl.binding.RunTest;
import org.openl.module.ModuleTest;
import org.openl.syntax.ParserTest;

/**
 * @author snshor
 *
 */
public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for openl");
        // $JUnit-BEGIN$
        suite.addTest(new TestSuite(ModuleTest.class));
        suite.addTest(new TestSuite(ParserTest.class));
        suite.addTest(new TestSuite(BinderTest.class));
        suite.addTest(new TestSuite(RunTest.class));

        suite.addTest(new TestSuite(OpenlToolTest.class));

        // $JUnit-END$
        return suite;
    }
}
