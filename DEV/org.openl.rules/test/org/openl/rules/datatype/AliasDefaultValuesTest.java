package org.openl.rules.datatype;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class AliasDefaultValuesTest extends BaseOpenlBuilderHelper {

    private static final String src = "test/rules/datatype/AliasDefaultValues.xls";

    public AliasDefaultValuesTest() {
        super(src);
    }

    @Before
    public void testBefore() {
        testNoErrors();
    }

    @SuppressWarnings("deprecation")
    private void testNoErrors() {
        Assert.assertTrue("No binding errors", getCompiledOpenClass().getBindingErrors().length == 0);
        Assert.assertTrue("No parsing errors", getCompiledOpenClass().getParsingErrors().length == 0);
        Assert.assertTrue("No messages", getCompiledOpenClass().getMessages().isEmpty());
    }

    @Test
    public void testValidDefaultValues() {
        try {
            Class<?> clazz = getClass("org.openl.generated.beans.ValidDefaultValues");
            Object instance = clazz.newInstance();

            testValue(clazz, instance, "getStr", "CA");
            testValue(clazz, instance, "getIntVal", 2);
            testValue(clazz, instance, "getIntegerVal", 257);
            testValue(clazz, instance, "getFlag", true);
            testValue(clazz, instance, "getStr2", "AR Track");
            testValue(clazz, instance, "getIntVal2", -1);
            testValue(clazz, instance, "getIntegerVal2", -2);
            testValue(clazz, instance, "getFlag2", true);

        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testNoDefaultValues() {
        try {
            Class<?> clazz = getClass("org.openl.generated.beans.NoDefaultValues");
            Object instance = clazz.newInstance();

            testValue(clazz, instance, "getStr", null);
            testValue(clazz, instance, "getIntVal", 0);
            testValue(clazz, instance, "getIntegerVal", null);
            testValue(clazz, instance, "getFlag", false);

        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    private void testValue(Class<?> clazz, Object instance, String methodName, Object expectedResult) throws NoSuchMethodException,
            IllegalAccessException,
            InvocationTargetException {
        Method method = clazz.getMethod(methodName, new Class<?>[0]);
        assertNotNull(method);
        Object result = method.invoke(instance, new Object[0]);
        assertEquals(expectedResult, result);
    }
}
