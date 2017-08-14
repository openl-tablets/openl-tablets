package org.openl.rules.testmethod;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;


public class InstantiationWithTestMethodOverridingTest {
    private static final String FILE_NAME = "test\\rules\\engine\\InstantiationWithTestMethodOverriding.xlsx";

    @Test
    public void test() {
        RulesEngineFactory<Object> engineFactory = new RulesEngineFactory<Object>(FILE_NAME);
        engineFactory.setExecutionMode(false);
        
        Object o = engineFactory.newInstance();
        
        Assert.assertNotNull(o);
    }

}
