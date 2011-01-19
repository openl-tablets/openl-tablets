/*
 * Created on Oct 2, 2003
 *
 *  Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.IOpenBinder;
import org.openl.IOpenParser;
import org.openl.OpenL;
import org.openl.binding.IBoundCode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.test.OpenlTest;
import org.openl.xls.OpenLBuilder;

/**
 * @author snshor
 * 
 */
public class TestParser extends TestCase {

    private int testOpenlParse(String fileName) throws Exception {

        System.out.println(OpenLBuilder.class.getName());

        OpenL openl = OpenL.getInstance("org.openl.xls");
        IOpenParser parser = openl.getParser();

        FileSourceCodeModule scm = new FileSourceCodeModule(new File(fileName), null);

        IParsedCode pc = parser.parseAsModule(scm);

        SyntaxNodeException[] err = pc.getErrors();

        for (int i = 0; i < err.length; i++) {
            // don`t need to print stack trace during tests!
//            printSyntaxError(err[i]);
        }

        return err.length;
    }

    private int testOpenlBind(String fileName) throws Exception {

        OpenL openl = OpenL.getInstance("org.openl.xls");
        IOpenParser parser = openl.getParser();
        IOpenSourceCodeModule scm = new FileSourceCodeModule(new File(fileName), null);
        IParsedCode pc = parser.parseAsModule(scm);
        SyntaxNodeException[] err = pc.getErrors();

        for (int i = 0; i < err.length; i++) {
//            printSyntaxError(err[i]);
        }

        IOpenBinder binder = openl.getBinder();
        IBoundCode bc = binder.bind(pc);

        err = bc.getErrors();

        for (int i = 0; i < err.length; i++) {
//            printSyntaxError(err[i]);
        }

        return err.length;
    }

    private void printSyntaxError(SyntaxNodeException err) {

        System.out.println(err.getMessage());
        ((Throwable) err).printStackTrace();

        if (err.getOriginalCause() != null) {
            Throwable t = ExceptionUtils.getCause(err.getOriginalCause());
            t = t == null ? err.getOriginalCause() : t;
            t.printStackTrace();
        }
    }

    public void testOpenlParse1() throws Exception {
        
        URL url1 = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test1.xls");
        URL url2 = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test2.xls");
        
        Assert.assertEquals(0, testOpenlParse(url1.getPath()));
        Assert.assertEquals(0, testOpenlParse(url2.getPath()));
    }

    public void testOpenlParse2() throws Exception {
        
        URL url1 = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test1.xls");
        URL url2 = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test2-2.xls");
        
        Assert.assertEquals(0, testOpenlParse(url1.getPath()));
        Assert.assertEquals(0, testOpenlParse(url2.getPath()));
    }

    public void testOpenlBind1() throws Exception {
        
        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test2.xls");
        Assert.assertEquals(0, testOpenlBind(url.getPath()));
    }

    public void testOpenlBind2() throws Exception {
        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test2-2.xls");
        Assert.assertEquals(0, testOpenlBind(url.getPath()));
    }

//    public void testOpenlBind3() throws Exception {
//        
//        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/IndexLogic.xls");
//        
//        Assert.assertEquals(5, testOpenlBind(url.getPath()));
//    }

    public void testOpenlRun1() throws Exception {
        
        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test2.xls");
        OpenlTest.aTestMethodFile(url.getPath(), "org.openl.xls", "hello", new Object[] { new Integer(14) }, "Y5");
    }

    public void testOpenlRun2() throws Exception {
        
        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test2-2.xls");
        OpenlTest.aTestMethodFile(url.getPath(), "org.openl.xls", "hello", new Object[] { new Integer(10) }, null);
    }

	/**
	 * Ignore the test. Test rules table doesn't have success result for current
	 * input. If table property "failOnMiss" has "TRUE" value exception will be
	 * thrown at binding step.
	 */
	public void testOpenlRun3() throws Exception {
        
//        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/IndexLogic.xls");
//        OpenlTest.aTestMethodFile(url.getPath(), "org.openl.xls", "main", new Object[] { new String[] {} }, null);
	}
}