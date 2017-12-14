package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.types.IOpenField;
import org.openl.types.impl.ThisField;
import org.openl.types.java.JavaOpenClass;

import java.util.Collections;

public class TestUnitTest {

    @Test
    public void testResultValueModification() {
        TestDescription test = mock(TestDescription.class);
        when(test.isExpectedResultDefined()).thenReturn(true);
        when(test.getExpectedResult()).thenReturn(new DoubleValue(0.93));

        TestUnit unit = new TestUnit(test, 0.93, 100);
        unit.setFieldsToTest(Collections.<IOpenField>singletonList(new ThisField(JavaOpenClass.DOUBLE)));

        assertEquals(0.93, unit.getActualResult());
        assertEquals(TestStatus.TR_OK, unit.compareResult());
    }

}
