package org.openl.rules;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;

public class EnvTableTest {
    private static final String SRC = "test/rules/EnvTable.xls";

    @Test
    public void testImport() {
        RulesEngineFactory<?>  engineFactory = new RulesEngineFactory<Object>(SRC);
        Assert.assertFalse(engineFactory.getCompiledOpenClass().hasErrors()); //All imports are found. No errors in module.
    }

}
