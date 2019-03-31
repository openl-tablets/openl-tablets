package org.openl.rules.validation;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.validation.DimentionalPropertyValidator.OverlapState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class DimentionalPropertyValidatorTest {

    private DimentionalPropertyValidator validator;
    private OverlapState startState;
    private String[] vResult;

    @Before
    public void setUp() {
        validator = new DimentionalPropertyValidator();
        startState = OverlapState.UNKNOWN;
        vResult = new String[3];
    }

    @Test
    public void test_loopInternal_UNKNOWN_overlapState() {
        final String[] propA = new String[] { "A", "B", "C" };
        final String[] propB = new String[] { "A", "B", "C" };
        final String key = "somekey";

        final OverlapState actual = validator.loopInternal(startState, vResult, key, propA, propB);

        assertEquals(startState, actual);
        for (String anActual : vResult) {
            if (anActual != null) {
                fail("All elements must be null!");
            }
        }
    }

    @Test
    public void test_loopInternal_INCLUDE_TO_A_overlapState() {
        testIncludeToA("somekey", new String[] { "A", "B", "C" }, new String[] { "A", "B" });
        testIncludeToA("somekey", new String[] { "A", "B", "C" }, new String[] { "B", "C" });
        testIncludeToA("somekey", new String[] { "A", "B", "C" }, new String[] { "A" });
        testIncludeToA("somekey", new String[] { "A", "B", "C" }, new String[] { "B" });
        testIncludeToA("somekey", new String[] { "A", "B", "C" }, new String[] { "C" });
        testIncludeToA("somekey", new String[] { "A", "B", "C" }, new String[] { "A", "C" });
    }

    @Test
    public void test_loopInternal_INCLUDE_TO_B_overlapState() {
        testIncludeToB("somekey", new String[] { "A", "B" }, new String[] { "A", "B", "C" });
        testIncludeToB("somekey", new String[] { "B", "C" }, new String[] { "A", "B", "C" });
        testIncludeToB("somekey", new String[] { "A" }, new String[] { "A", "B", "C" });
        testIncludeToB("somekey", new String[] { "B" }, new String[] { "A", "B", "C" });
        testIncludeToB("somekey", new String[] { "C" }, new String[] { "A", "B", "C" });
        testIncludeToB("somekey", new String[] { "A", "C" }, new String[] { "A", "B", "C" });
    }

    @Test
    public void test_loopInternal_NOT_OVERLAP_overlapState() {
        testNotOverlap("somekey", new String[] { "A", "B", "C" }, new String[] { "D", "E", "F" });
    }

    @Test
    public void test_loopInternal_OVERLAP_overlapState() {
        testOverlap("somekey", new String[] { "A", "B", "C" }, new String[] { "A", "E", "F" });
        testOverlap("somekey", new String[] { "B", "C", "D" }, new String[] { "A", "B", "F" });
        testOverlap("somekey", new String[] { "B", "C", "D" }, new String[] { "A", "B", "F" });
        testOverlap("somekey", new String[] { "B", "C", "G" }, new String[] { "A", "F", "G" });
        testOverlap("somekey", new String[] { "B", "C", "D" }, new String[] { "A", "B" });
        testOverlap("somekey", new String[] { "A", "B" }, new String[] { "B", "C", "D" });
    }

    private void testIncludeToA(String key, String[] propA, final String[] propB) {
        final OverlapState actual = validator.loopInternal(startState, vResult, key, propA, propB);

        assertEquals(OverlapState.INCLUDE_TO_A, actual);
        assertEquals(key, vResult[0]);
        for (int i = 1; i < 3; i++) {
            if (vResult[i] != null) {
                fail("All elements must be null!");
            }
        }
    }

    private void testIncludeToB(String key, String[] propA, final String[] propB) {
        final OverlapState actual = validator.loopInternal(startState, vResult, key, propA, propB);

        assertEquals(OverlapState.INCLUDE_TO_B, actual);
        assertEquals(key, vResult[1]);
        assertNull(vResult[0]);
        assertNull(vResult[2]);
    }

    private void testNotOverlap(String key, String[] propA, final String[] propB) {
        final OverlapState actual = validator.loopInternal(startState, vResult, key, propA, propB);

        assertEquals(OverlapState.NOT_OVERLAP, actual);
        for (String anActual : vResult) {
            if (anActual != null) {
                fail("All elements must be null!");
            }
        }
    }

    private void testOverlap(String key, String[] propA, final String[] propB) {
        final OverlapState actual = validator.loopInternal(startState, vResult, key, propA, propB);

        assertEquals(OverlapState.OVERLAP, actual);
        for (int i = 0; i < 2; i++) {
            if (vResult[i] != null) {
                fail("All elements must be null!");
            }
        }
    }

}
