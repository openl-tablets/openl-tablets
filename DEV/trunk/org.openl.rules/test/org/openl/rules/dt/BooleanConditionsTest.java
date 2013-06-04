package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.TestHelper;

public class BooleanConditionsTest {

    private ITestI instance;
    private static final String SRC = "test/rules/dt/BooleanConditions.xlsx";

    public interface ITestI {
        int testSimplifiedBooleanConditionsWithParam(int param1, int param2);

        int testBooleanConditionsWithoutParam(int param1, int param2);
    }

    @Before
    public void init() {
        File xlsFile = new File(SRC);
        TestHelper<ITestI> testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);

        instance = testHelper.getInstance();
    }

    @Test
    public void testSimplifiedBooleanConditionsWithParam() {
        assertEquals(4, instance.testSimplifiedBooleanConditionsWithParam(6, 7));
        assertEquals(2, instance.testSimplifiedBooleanConditionsWithParam(4, 7));
        assertEquals(3, instance.testSimplifiedBooleanConditionsWithParam(6, 12));
        assertEquals(1, instance.testSimplifiedBooleanConditionsWithParam(4, 12));
    }

    @Test
    public void testSimplifiedBooleanConditionsWithoutParam() {
        assertEquals(1, instance.testBooleanConditionsWithoutParam(6, 7));
        assertEquals(3, instance.testBooleanConditionsWithoutParam(4, 7));
        assertEquals(2, instance.testBooleanConditionsWithoutParam(6, 12));
        assertEquals(4, instance.testBooleanConditionsWithoutParam(4, 12));
    }
}
