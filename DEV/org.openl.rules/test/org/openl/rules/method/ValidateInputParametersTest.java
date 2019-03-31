package org.openl.rules.method;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ValidateInputParametersTest {
    private static final String SRC = "test/rules/ValidateInputParameters.xls";

    private RulesEngineFactory<Object> engineFactory;

    @Before
    public void init() throws IOException {
        engineFactory = new RulesEngineFactory<Object>(SRC);
    }

    @Test(expected = OutsideOfValidDomainException.class)
    public void test() {
        IOpenMethod method = engineFactory.getCompiledOpenClass()
            .getOpenClass()
            .getMethod("SHTable", new IOpenClass[] { JavaOpenClass.STRING });

        Assert.assertNotNull(method);

        Object target = engineFactory.newEngineInstance();
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        method.invoke(target, new Object[] { "ONE2" }, env);

    }

    @Test(expected = OutsideOfValidDomainException.class)
    public void testArray() {
        IOpenMethod method = engineFactory.getCompiledOpenClass()
            .getOpenClass()
            .getMethod("DTTable2",
                new IOpenClass[] { JavaOpenClass.getOpenClass(JavaOpenClass.makeArrayClass(String.class)) });

        Assert.assertNotNull(method);

        Object target = engineFactory.newEngineInstance();
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        method.invoke(target, new Object[] { new String[] { "ONE", "ONE2" } }, env);

    }
}
