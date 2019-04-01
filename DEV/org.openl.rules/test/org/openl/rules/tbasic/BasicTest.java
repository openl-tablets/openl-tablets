package org.openl.rules.tbasic;

import org.junit.Ignore;
import org.openl.rules.runtime.RulesEngineFactory;

@Ignore("Manual test")
public class BasicTest {
    private static final String SRC = "test/rules/BasicAlgorithm.xls";

    public static void main(String[] args) {
        RulesEngineFactory<IBasicTest> engineFactory = new RulesEngineFactory<>(SRC, IBasicTest.class);

        IBasicTest rule = engineFactory.newEngineInstance();

        int result = rule.modification(4);

        System.out.println(result);
    }
}

interface IBasicTest {
    int modification(int x);
}