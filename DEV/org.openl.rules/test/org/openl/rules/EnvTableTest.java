package org.openl.rules;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;

public class EnvTableTest {
    private static final String SRC = "test/rules/EnvTable.xls";

    @Test
    public void testImport() {
        File xlsFile = new File(SRC);
        RulesEngineFactory<?>  engineFactory = new RulesEngineFactory<Object>(xlsFile);
        Assert.assertFalse(engineFactory.getCompiledOpenClass().hasErrors()); //All imports are found. No errors in module.
    }

}
