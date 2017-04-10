package org.openl.rules;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.runtime.RulesEngineFactory;

public class NullSafetyTest {
    private static final String SRC = "test/rules/NullSafetyTest.xlsx";

    @Test
    public void testImport() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(SRC);
        Assert.assertFalse(engineFactory.getCompiledOpenClass().hasErrors()); // No
                                                                              // errors
                                                                              // in
                                                                              // module.

        Object target = engineFactory.newInstance();
        try {
            Method method = target.getClass().getMethod("NullSafety");
            SpreadsheetResult spreadsheetResult = (SpreadsheetResult) method.invoke(target);
            
            Assert.assertNull(spreadsheetResult.getFieldValue("$Value$NullSafeAction"));
        } catch (NoSuchMethodException e) {
            Assert.fail();
        } catch (InvocationTargetException e) {
            Assert.fail();
        } catch (IllegalAccessException e) {
            Assert.fail();
        }
    }
}
