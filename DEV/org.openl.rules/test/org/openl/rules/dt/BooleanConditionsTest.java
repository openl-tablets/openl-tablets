package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

public class BooleanConditionsTest {

    private static final String SRC = "test/rules/dt/BooleanConditions.xlsx";
    private ITestI instance;

    @BeforeEach
    public void init() {
        instance = TestUtils.create(SRC, ITestI.class);
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

    public interface ITestI {
        int testSimplifiedBooleanConditionsWithParam(int param1, int param2);

        int testBooleanConditionsWithoutParam(int param1, int param2);
    }
}
