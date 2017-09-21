package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openl.meta.DoubleValue;

public class TestUnitTest {

    @Test
    public void testResultValueModification() {
        TestDescription test = mock(TestDescription.class);
        when(test.isExpectedResultDefined()).thenReturn(true);
        when(test.getExpectedResult()).thenReturn(new DoubleValue(0.9));

        DoubleValue testResult = new DoubleValue(0.93);

        TestUnit unit = new TestUnit(test, testResult, 100);

        assertEquals(new DoubleValue(0.93), unit.getActualResult());
        assertEquals(0, unit.compareResult());

        // check that original value is not changed
        assertEquals(new DoubleValue(0.93), testResult);
    }

}
