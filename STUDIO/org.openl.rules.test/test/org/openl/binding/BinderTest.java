package org.openl.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.conf.OpenLConfigurationException;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

/*
 * Created on May 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

/**
 * @author snshor
 */
public class BinderTest {

    private void _testMethodHeader(String code, IOpenClass type, int numPar) {
        OpenL openl = OpenL.getInstance();
        IOpenMethodHeader header = OpenLManager
                .makeMethodHeader(openl, new StringSourceCodeModule(code, null), openl.getBinder().makeBindingContext());
        assertEquals(type, header.getType());
        assertEquals(numPar, header.getSignature().getParameterTypes().length);
    }

    private void _testNoError(String testCode, Class<?> targetClass) throws OpenLConfigurationException {

        OpenL op = OpenL.getInstance();

        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(testCode, null));

        int errnum = pc.getErrors().length;

        for (int i = 0; i < errnum; i++) {
            SyntaxNodeException err = pc.getErrors()[i];
            System.out.println(err);
        }

        assertEquals(0, errnum);

        IOpenBinder b = op.getBinder();

        IBoundCode bc = b.bind(pc);

        errnum = bc.getErrors().length;

        for (int i = 0; i < errnum; i++) {
            SyntaxNodeException err = bc.getErrors()[i];
            System.out.println(err);
        }

        assertEquals(0, errnum);

        assertEquals(targetClass, bc.getTopNode().getType().getInstanceClass());
    }

    @Test
    public void testBind() throws OpenLConfigurationException {
        _testNoError("String[] name;", void.class);
        _testNoError("int x = 5, z, y= 20;", void.class);
        _testNoError("5.5", double.class);
        _testNoError("5.5 + 4.5", double.class);
        _testNoError("5.5 + 4", double.class);
        _testNoError("\t545847548567L", long.class);
        _testNoError("4+3", int.class);
        _testNoError("\t-545847548567L", long.class);
        _testNoError("5-3", int.class);
        _testNoError("int x = 5, z, y= 20; x < 3 || z > 2", Boolean.class);
        _testNoError("Date d1, d2; d1 < d2", Boolean.class);
        _testNoError("String[] name;", void.class);
    }

    @Test
    public void testMeta() {
        _testNoError("DoubleValue d1, d2; d1 + d2", Double.class);
    }

    @Test
    public void testMethodHeader() throws OpenLCompilationException {
        _testMethodHeader("int x()", JavaOpenClass.INT, 0);
        _testMethodHeader("void x(int zz, double aa)", JavaOpenClass.VOID, 2);
    }

}
