/*
 * Created on Oct 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.test;

import java.io.File;
import java.io.FileReader;

import junit.framework.Assert;

import org.openl.IOpenParser;
import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLRuntimeException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class OpenlTest {

    public static void aTestEvaluate(String code, Object result, String openl) throws Exception {
        OpenL op = OpenL.getInstance(openl);

        System.out.print(code + " = ");

        Object res = OpenLManager.runScript(op, new StringSourceCodeModule(code, null));

        System.out.println(res);

        Assert.assertEquals(result, res);
    }

    public static void aTestEvaluateFile(String filename, Object result, String openl) throws Exception {

        FileReader reader = new FileReader(filename);

        int c;
        StringBuffer buf = new StringBuffer();
        while ((c = reader.read()) != -1) {
            buf.append((char) c);
        }

        aTestEvaluate(buf.toString(), result, openl);

    }

    public static void aTestEvaluateHasError(String code, String openl) {
        OpenL op = OpenL.getInstance(openl);
        boolean failed = false;
        try {
            OpenLManager.runScript(op, new StringSourceCodeModule(code, null));
        } catch (Throwable t) {
            failed = true;
        }
        Assert.assertTrue(failed);

    }

    static public void aTestEvaluateMethod(String moduleCode, String openl, String methodName, Object[] params,
            Object expected) throws Exception {
        OpenL op = OpenL.getInstance(openl);

        Class<?>[] cc = new Class[params.length];
        for (int i = 0; i < cc.length; i++) {
            cc[i] = params[i].getClass();
            if (cc[i] == Integer.class) {
                cc[i] = int.class;
            }
        }

        Object res = OpenLManager.runMethod(op, new StringSourceCodeModule(moduleCode, null), methodName, JavaOpenClass
                .getOpenClasses(cc), params);

        Assert.assertEquals(expected, res);

    }

    static public void aTestMethodFile(String moduleFile, String openl, String methodName, Object[] params,
            Object expected) throws Exception {

        Object res = evaluateFile(moduleFile, openl, methodName, params);
        Assert.assertEquals(expected, res);

    }

    public static int aTestOpenlParse(String fileName, String openlName) throws Exception {
        OpenL openl = OpenL.getInstance(openlName);

        IOpenParser parser = openl.getParser();

        FileSourceCodeModule scm = new FileSourceCodeModule(new File(fileName), null);

        IParsedCode pc = parser.parseAsModule(scm);

        SyntaxNodeException[] err = pc.getErrors();
        for (int i = 0; i < err.length; i++) {
            System.out.println(err[i].getLocation() + " -- " + err[i]);
        }

        return err.length;
    }

    public static Object evaluate(IOpenSourceCodeModule moduleCode, String openl, String methodName, Object[] params)
            throws Exception {
        OpenL op = OpenL.getInstance(openl);

        Class<?>[] cc = new Class[params.length];
        for (int i = 0; i < cc.length; i++) {
            cc[i] = params[i].getClass();
            if (cc[i] == Integer.class) {
                cc[i] = int.class;
            }
        }

        Object res = OpenLManager.runMethod(op, moduleCode, methodName, JavaOpenClass.getOpenClasses(cc), params);

        return res;
    }

    public static Object evaluate(String code, String openl) throws OpenLRuntimeException {
        OpenL op = OpenL.getInstance(openl);
        return OpenLManager.runScript(op, new StringSourceCodeModule(code, null));
    }

    public static Object evaluateFile(String filename, String openl) throws Exception {

        System.out.println("Evaluating: " + filename);

        FileReader reader = new FileReader(filename);

        int c;
        StringBuffer buf = new StringBuffer();
        while ((c = reader.read()) != -1) {
            buf.append((char) c);
        }

        return evaluate(buf.toString(), openl);

    }

    public static Object evaluateFile(String filename, String openl, String methodName, Object[] params)
            throws Exception {

        return evaluate(new FileSourceCodeModule(new File(filename), null), openl, methodName, params);

    }

}
