package org.openl.binding;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.conf.OpenConfigurationException;
import org.openl.engine.OpenLManager;
import org.openl.meta.DoubleValue;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.DynamicObject;
import org.openl.types.java.JavaOpenClass;

/*
 * Created on May 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

/**
 * @author snshor
 *
 */
public class BinderTest extends TestCase {

    /**
     * Constructor for BinderTest.
     *
     * @param arg0
     */
    public BinderTest(String arg0) {
        super(arg0);
    }

    public void _testMethodHeader(String code, IOpenClass type, String openlName, int numPar) {
        OpenL openl = OpenL.getInstance(openlName);
        IOpenMethodHeader header = OpenLManager.makeMethodHeader(openl, new StringSourceCodeModule(code, null), null);
        Assert.assertEquals(type, header.getType());
        Assert.assertEquals(numPar, header.getSignature().getParameterTypes().length);
    }

    public void _testNoError(String testCode, Class<?> targetClass, String parser) throws OpenConfigurationException {

        OpenL op = OpenL.getInstance(parser);

        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(testCode, null));

        int errnum = pc.getErrors().length;

        for (int i = 0; i < errnum; i++) {
            SyntaxNodeException err = pc.getErrors()[i];
            System.out.println(err);
        }

        Assert.assertEquals(0, errnum);

        IOpenBinder b = op.getBinder();

        IBoundCode bc = b.bind(pc);

        errnum = bc.getErrors().length;

        for (int i = 0; i < errnum; i++) {
            SyntaxNodeException err = bc.getErrors()[i];
            System.out.println(err);
        }

        Assert.assertEquals(0, errnum);

        Assert.assertEquals(targetClass, bc.getTopNode().getType().getInstanceClass());
    }

    public void _testNoErrorModule(String testCode, Class<?> targetClass, String parser)
            throws OpenConfigurationException {

        OpenL op = OpenL.getInstance(parser);

        IParsedCode pc = op.getParser().parseAsModule(new StringSourceCodeModule(testCode, null));

        int errnum = pc.getErrors().length;

        for (int i = 0; i < errnum; i++) {
            SyntaxNodeException err = pc.getErrors()[i];
            System.out.println(err);
        }

        Assert.assertEquals(0, errnum);

        IOpenBinder b = op.getBinder();

        IBoundCode bc = b.bind(pc);

        errnum = bc.getErrors().length;

        for (int i = 0; i < errnum; i++) {
            SyntaxNodeException err = bc.getErrors()[i];
            System.out.println(err);
        }

        Assert.assertEquals(0, errnum);

        Assert.assertEquals(targetClass, bc.getTopNode().getType().getInstanceClass());
    }

    public void testBind() throws OpenConfigurationException {
        _testNoError("String[] name;", void.class, "org.openl.j");
        _testNoErrorModule("int foo(int x){return x+y+5;} int y= 3;", DynamicObject.class, "org.openl.j");

        _testNoError("int x = 5, z, y= 20;", void.class, "org.openl.j");
        _testNoError("5.5", double.class, "org.openl.j");
        _testNoError("5.5 + 4.5", double.class, "org.openl.j");
        _testNoError("5.5 + 4", double.class, "org.openl.j");
        _testNoError("\t545847548567L", long.class, "org.openl.j");
        _testNoError("4+3", int.class, "org.openl.j");
        _testNoError("\t-545847548567L", long.class, "org.openl.j");
        _testNoError("5-3", int.class, "org.openl.j");
        _testNoError("int x = 5, z, y= 20; x < 3 || z > 2", boolean.class, "org.openl.j");
        _testNoError("Date d1, d2; d1 < d2", boolean.class, "org.openl.j");
        // _testNoError("Date[] d1, d2", boolean.class, "org.openl.j");
        _testNoError("String[] name;", void.class, "org.openl.j");

    }

    public void testMeta() {
        _testNoError("DoubleValue d1, d2; d1 + d2", DoubleValue.class, "org.openl.rules.java");
    }

    public void testMethodHeader() {
        _testMethodHeader("int x()", JavaOpenClass.INT, "org.openl.j", 0);
        _testMethodHeader("void x(int zz, double aa)", JavaOpenClass.VOID, "org.openl.j", 2);
    }

}
