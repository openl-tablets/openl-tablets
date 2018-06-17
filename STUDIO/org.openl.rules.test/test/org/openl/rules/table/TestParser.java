/*
 * Created on Oct 2, 2003
 *
 *  Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

import java.net.URL;

import junit.framework.TestCase;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Assert;
import org.openl.IOpenBinder;
import org.openl.IOpenParser;
import org.openl.OpenL;
import org.openl.binding.IBoundCode;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.java.JavaOpenClass;
import org.openl.xls.OpenLBuilder;

/**
 * @author snshor
 * 
 */
public class TestParser extends TestCase {

    private int testOpenlParse(URL fileName) throws Exception {

        System.out.println(OpenLBuilder.class.getName());

        OpenL openl = OpenL.getInstance(OpenL.OPENL_JAVA_RULE_NAME);
        IOpenParser parser = openl.getParser();

        URLSourceCodeModule scm = new URLSourceCodeModule(fileName);

        IParsedCode pc = parser.parseAsModule(scm);

        SyntaxNodeException[] err = pc.getErrors();

        for (int i = 0; i < err.length; i++) {
            // don`t need to print stack trace during tests!
            // printSyntaxError(err[i]);
        }

        return err.length;
    }

    private int testOpenlBind(URL fileName) throws Exception {

        OpenL openl = OpenL.getInstance(OpenL.OPENL_JAVA_RULE_NAME);
        IOpenParser parser = openl.getParser();
        IOpenSourceCodeModule scm = new URLSourceCodeModule(fileName);
        IParsedCode pc = parser.parseAsModule(scm);
        SyntaxNodeException[] err = pc.getErrors();

        for (int i = 0; i < err.length; i++) {
            // printSyntaxError(err[i]);
        }

        IOpenBinder binder = openl.getBinder();
        IBoundCode bc = binder.bind(pc);

        err = bc.getErrors();

        for (int i = 0; i < err.length; i++) {
            // printSyntaxError(err[i]);
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

        Assert.assertEquals(0, testOpenlParse(url1));
        Assert.assertEquals(0, testOpenlParse(url2));
    }

    public void testOpenlParse2() throws Exception {

        URL url1 = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test1.xls");
        URL url2 = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test2-2.xls");

        Assert.assertEquals(0, testOpenlParse(url1));
        Assert.assertEquals(0, testOpenlParse(url2));
    }

    public void testOpenlBind1() throws Exception {

        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test2.xls");
        Assert.assertEquals(0, testOpenlBind(url));
    }

    public void testOpenlBind2() throws Exception {
        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test2-2.xls");
        Assert.assertEquals(0, testOpenlBind(url));
    }

    public void testOpenlRun1() throws Exception {
        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test2.xls");
        aTestMethodFile(url, OpenL.OPENL_JAVA_RULE_NAME, "hello", new Object[] { new Integer(14) }, "Y5");
    }

    public void testOpenlRun2() throws Exception {

        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/Test2-2.xls");
        aTestMethodFile(url, OpenL.OPENL_JAVA_RULE_NAME, "hello", new Object[] { new Integer(10) }, null);
    }

    private void aTestMethodFile(URL moduleFile,
            String openl,
            String methodName,
            Object[] params,
            Object expected) throws Exception {

        OpenL op = OpenL.getInstance(openl);

        Class<?>[] cc = new Class[params.length];
        for (int i = 0; i < cc.length; i++) {
            cc[i] = params[i].getClass();
            if (cc[i] == Integer.class) {
                cc[i] = int.class;
            }
        }

        Object res = OpenLManager
            .runMethod(op, new URLSourceCodeModule(moduleFile), methodName, JavaOpenClass.getOpenClasses(cc), params);

        Assert.assertEquals(expected, res);

    }
}
