package org.openl.rules.engine;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.context.IRulesRuntimeContextProvider;

public class FullClassNameSupportTest {
    private static final String SRC = "test/rules/engine/fullJavaClassNameSupportTest.xls";
    
    public interface ITestI extends IRulesRuntimeContextProvider {
        boolean test1();
        boolean test2();
    }
    
    @Test
    public void test() {
        File xlsFile = new File(SRC);
        TestHelper<ITestI> testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);
        
        Assert.assertEquals(false, testHelper.getTableSyntaxNode().hasErrors());
        
        ITestI instance = testHelper.getInstance();
        
        Assert.assertEquals(true, instance.test1());
        Assert.assertEquals(true, instance.test2());
        
    }
}
