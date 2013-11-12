/*
 * Created on Jun 18, 2003
 *
 *  Developed by OpenRules Inc. 2003
 */

package org.openl.types.xml;

import org.openl.test.OpenlTest;

import junit.framework.TestCase;

/**
 * @author snshor
 *
 */
public class DOMTest extends TestCase {

    private static final String OPENL_J_XML_DOM_NAME = "org.openl.j.xml.dom";

    /**
     * Constructor for DOMTest.
     *
     * @param arg0
     */
    public DOMTest(String arg0) {
        super(arg0);
    }

    public void _testDomOpenLib() throws Exception {

        OpenlTest.aTestEvaluate("test1.ary.add(new test1.ary())", null, OPENL_J_XML_DOM_NAME);
        OpenlTest.aTestEvaluate("test1.ary.length", new Integer(2), OPENL_J_XML_DOM_NAME);
        OpenlTest.aTestEvaluate("test1.ary[1]._cdata_", "ary1", OPENL_J_XML_DOM_NAME);
        OpenlTest.aTestEvaluate("test1.ary[0]._cdata_", null, OPENL_J_XML_DOM_NAME);
        OpenlTest.aTestEvaluate("test1.ary[0].name", "ary0", OPENL_J_XML_DOM_NAME);
        OpenlTest.aTestEvaluate("test1.value", "test3", OPENL_J_XML_DOM_NAME);
        OpenlTest.aTestEvaluate("test1.test2._cdata_", "abc", OPENL_J_XML_DOM_NAME);

        OpenlTest.aTestEvaluateHasError("test1.ary._cdata_", OPENL_J_XML_DOM_NAME);
        OpenlTest.aTestEvaluateHasError("test1.b._cdata_", OPENL_J_XML_DOM_NAME);
    }

    public void testDOMSchema() throws Exception {
       // This feature is not supported yet.
       //
       // OpenlTest.aTestEvaluateFile("tst_openl.j/org/openl/types/xml/DOMTest1.txt", "abc", "domtest.1");
    }

    public void testSave() throws Exception {
        // This feature is not supported yet.
    	//
    	// OpenlTest.evaluateFile("tst_openl.j/org/openl/types/xml/DOMTest2.txt", "domtest.1");
    	// OpenlTest.evaluateFile("tst_openl.j/org/openl/types/xml/AutoPolicy1write.test.openl", "domtest.1");
    	// OpenlTest.evaluateFile("tst_openl.j/org/openl/types/xml/AutoPolicy1read.test.openl", "domtest.1");
    }

}
