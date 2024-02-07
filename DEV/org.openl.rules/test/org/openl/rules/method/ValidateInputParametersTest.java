package org.openl.rules.method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    public void init() throws IOException {
        engineFactory = new RulesEngineFactory<>(SRC);
    }

    @Test
    public void test() {
        assertThrows(OutsideOfValidDomainException.class, () -> {
            IOpenMethod method = engineFactory.getCompiledOpenClass()
                    .getOpenClass()
                    .getMethod("SHTable", new IOpenClass[]{JavaOpenClass.STRING});

            assertNotNull(method);

            Object target = engineFactory.newEngineInstance();
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            method.invoke(target, new Object[]{"ONE2"}, env);

        });

    }

    @Test
    public void testArray() {
        assertThrows(OutsideOfValidDomainException.class, () -> {
            IOpenMethod method = engineFactory.getCompiledOpenClass()
                    .getOpenClass()
                    .getMethod("DTTable2", new IOpenClass[]{JavaOpenClass.getOpenClass(String[].class)});

            assertNotNull(method);

            Object target = engineFactory.newEngineInstance();
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            method.invoke(target, new Object[]{new String[]{"ONE", "ONE2"}}, env);

        });

    }
}
