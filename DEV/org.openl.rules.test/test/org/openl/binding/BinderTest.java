package org.openl.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.OpenL;
import org.openl.conf.OpenLConfigurationException;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.source.impl.StringSourceCodeModule;
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
class BinderTest {

    private void _testMethodHeader(String code, IOpenClass type, int numPar) {
        OpenL openl = OpenL.getInstance();
        IOpenMethodHeader header = OpenLManager
                .makeMethodHeader(openl, new StringSourceCodeModule(code, null), openl.getBinder().makeBindingContext());
        assertEquals(type, header.getType());
        assertEquals(numPar, header.getSignature().getParameterTypes().length);
    }

    private void _testNoError(String testCode, Class<?> targetClass) throws OpenLConfigurationException {

        OpenL op = OpenL.getInstance();

        var pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(testCode, null));

        var errnum = pc.getErrors().length;

        for (var i = 0; i < errnum; i++) {
            var err = pc.getErrors()[i];
            System.out.println(err);
        }

        assertEquals(0, errnum);

        var b = op.getBinder();

        var bc = b.bind(pc);

        errnum = bc.getErrors().length;

        for (var i = 0; i < errnum; i++) {
            var err = bc.getErrors()[i];
            System.out.println(err);
        }

        assertEquals(0, errnum);

        assertEquals(targetClass, bc.getTopNode().getType().getInstanceClass());
    }

    @Test
    void testBind() throws OpenLConfigurationException {
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
    void testMeta() {
        _testNoError("DoubleValue d1, d2; d1 + d2", Double.class);
    }

    @Test
    void testMethodHeader() throws OpenLCompilationException {
        _testMethodHeader("int x()", JavaOpenClass.INT, 0);
        _testMethodHeader("void x(int zz, double aa)", JavaOpenClass.VOID, 2);
    }

}
