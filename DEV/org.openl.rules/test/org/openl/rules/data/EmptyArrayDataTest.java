package org.openl.rules.data;

import java.io.File;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;

public class EmptyArrayDataTest {

    private static final String SRC = "test/rules/data/EmptyArrayTest.xls";

    @Test
    public void testMultiRowArrayLoad() throws Exception {
        File xlsFile = new File(SRC);

        RulesEngineFactory<Object> engineFactory = new RulesEngineFactory<Object>(xlsFile);
        engineFactory.setExecutionMode(true);

        Object instance = engineFactory.newEngineInstance();

        Class<?> clazz = engineFactory.getInterfaceClass();

        Method getPoliciesMethod = clazz.getMethod("getPolicies");

        Class<?> policyClazz = engineFactory.getCompiledOpenClass()
            .getClassLoader()
            .loadClass("org.openl.generated.beans.Policy");

        Object[] policies = (Object[]) getPoliciesMethod.invoke(instance);

        Assert.assertEquals(2, policies.length);

        Method getDriversMethod = policyClazz.getMethod("getDrivers");

        String[] policy1Drivers = (String[]) getDriversMethod.invoke(policies[0]);

        Assert.assertEquals(3, policy1Drivers.length);
        Assert.assertEquals("28", policy1Drivers[2]);

        String[] policy2Drivers = (String[]) getDriversMethod.invoke(policies[1]);
        Assert.assertEquals(0, policy2Drivers.length);

    }
}