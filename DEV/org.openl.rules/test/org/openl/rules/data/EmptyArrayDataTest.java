package org.openl.rules.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;

class EmptyArrayDataTest {

    private static final String SRC = "test/rules/data/EmptyArrayTest.xls";

    @Test
    void testMultiRowArrayLoad() throws Exception {
        var engineFactory = new RulesEngineFactory<Object>(SRC);
        engineFactory.setExecutionMode(true);

        var instance = engineFactory.newEngineInstance();

        Class<?> clazz = engineFactory.getInterfaceClass();

        var getMyDatasMethod = clazz.getMethod("getMyDatas");

        var policyClazz = engineFactory.getCompiledOpenClass()
                .getClassLoader()
                .loadClass("org.openl.generated.beans.MyData");

        var myDatas = (Object[]) getMyDatasMethod.invoke(instance);

        assertEquals(2, myDatas.length);

        var getStringsMethod = policyClazz.getMethod("getStrings");

        var strings1 = (String[]) getStringsMethod.invoke(myDatas[0]);

        assertEquals(3, strings1.length);
        assertEquals("28", strings1[2]);

        var strings2 = (String[]) getStringsMethod.invoke(myDatas[1]);
        assertEquals(0, strings2.length);

        var getPrimitivesMethod = policyClazz.getMethod("getPrimitives");

        var primitives = (long[]) getPrimitivesMethod.invoke(myDatas[0]);
        assertEquals(0, primitives.length);

        var getPrimitives2Method = policyClazz.getMethod("getPrimitives2");

        var primitives2 = (int[][]) getPrimitives2Method.invoke(myDatas[0]);
        assertEquals(0, primitives2.length);
    }
}
