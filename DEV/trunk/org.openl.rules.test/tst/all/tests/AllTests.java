/*
 * Created on May 19, 2003
 *
 *  Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package all.tests;

import org.openl.rules.lang.xls.TestParser;
import org.openl.rules.lang.xls.TestSingleXLS;
import org.openl.rules.table.TablesTest;


import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author snshor
 *
 */
public class AllTests
{

  public static Test suite()
  {
    TestSuite suite = new TestSuite("Test for openl");
    //$JUnit-BEGIN$
		suite.addTest(new TestSuite(TablesTest.class));
		suite.addTest(new TestSuite(TestParser.class));
		suite.addTest(new TestSuite(TestSingleXLS.class));


		
    //$JUnit-END$
    return suite;
  }
}
