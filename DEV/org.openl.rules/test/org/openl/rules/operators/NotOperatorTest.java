package org.openl.rules.operators;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestHelper;

public class NotOperatorTest {

    private static final String SRC = "test/rules/operators/NotOperatorTest.xlsx";

    private static RulesInterf instance;

    public interface RulesInterf {
        String testNotInt(int v1, int v2);
    }

    @BeforeClass
    public static void init() {
        File xlsFile = new File(SRC);
        TestHelper<RulesInterf> testHelper = new TestHelper<RulesInterf>(xlsFile, RulesInterf.class);

        instance = testHelper.getInstance();
    }

    @Test
    public void testNot() {
        assertEquals("passed", instance.testNotInt((int) 22, (int) 11));
        assertEquals("not passed", instance.testNotInt((int) 3, (int) 2));
    }

}
