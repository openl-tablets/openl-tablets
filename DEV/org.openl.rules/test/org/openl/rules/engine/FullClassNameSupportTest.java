package org.openl.rules.engine;

import static junit.framework.TestCase.assertTrue;

import org.junit.Test;
import org.openl.rules.TestUtils;
import org.openl.rules.context.IRulesRuntimeContextProvider;

public class FullClassNameSupportTest {
    private static final String SRC = "test/rules/engine/fullJavaClassNameSupportTest.xls";

    @Test
    public void test() {
        ITestI instance = TestUtils.create(SRC, ITestI.class);

        assertTrue(instance.test1());
        assertTrue(instance.test2());
    }

    public interface ITestI extends IRulesRuntimeContextProvider {
        boolean test1();

        boolean test2();
    }
}
