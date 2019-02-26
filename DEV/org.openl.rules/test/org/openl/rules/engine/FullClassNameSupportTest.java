package org.openl.rules.engine;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;

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
        TestHelper<ITestI> testHelper = new TestHelper<>(xlsFile, ITestI.class);

        assertFalse(testHelper.getTableSyntaxNode().hasErrors());

        ITestI instance = testHelper.getInstance();

        assertTrue(instance.test1());
        assertTrue(instance.test2());
    }
}
