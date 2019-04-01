package org.openl.rules.runtime;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

public class RulesFactoryTest {

    @Test
    public void testGenerateInterface1() throws Exception {

        String className = "my.test.TestInterfaceClass";
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        RuleInfo[] rules = new RuleInfo[2];

        RuleInfo rule1 = new RuleInfo();
        rule1.setName("method1");
        rule1.setReturnType(String.class);
        rule1.setParamTypes(new Class<?>[0]);
        rules[0] = rule1;

        RuleInfo rule2 = new RuleInfo();
        rule2.setName("method2");
        rule2.setReturnType(Void.class);
        rule2.setParamTypes(new Class<?>[] { String.class, Integer.class });
        rules[1] = rule2;

        Class<?> clazz = InterfaceGenerator.generateInterface(className, rules, classLoader);

        assertNotNull(clazz);
        assertTrue(clazz.isInterface());
        assertEquals(className, clazz.getName());

        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if ("method1".equals(method.getName())) {
                assertEquals("method1", method.getName());
                Class<?>[] parameterTypes1 = method.getParameterTypes();
                assertEquals(0, parameterTypes1.length);
                assertEquals(String.class, method.getReturnType());
            }
            if ("method2".equals(method.getName())) {
                assertEquals("method2", method.getName());
                Class<?>[] parameterTypes2 = method.getParameterTypes();
                assertEquals(2, parameterTypes2.length);
                assertEquals(String.class, parameterTypes2[0]);
                assertEquals(Integer.class, parameterTypes2[1]);
                assertEquals(Void.class, method.getReturnType());
            }
        }
    }
}
