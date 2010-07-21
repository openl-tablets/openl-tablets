package org.openl.rules.tbasic;

import org.junit.Ignore;
import org.openl.runtime.EngineFactory;

@Ignore("Manual test")
public class BasicTest {
    public static void main(String[] args) {
        EngineFactory<IBasicTest> engineFactory = new EngineFactory<IBasicTest>("org.openl.xls",
                "test/rules/BasicAlgorithm.xls", IBasicTest.class);

        IBasicTest rule = engineFactory.makeInstance();

        int result = rule.modification(4);

        System.out.println(result);
    }
}

interface IBasicTest {
    int modification(int x);
}