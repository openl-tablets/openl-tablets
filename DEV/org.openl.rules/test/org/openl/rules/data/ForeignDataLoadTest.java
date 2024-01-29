package org.openl.rules.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

public class ForeignDataLoadTest {

    @Test
    public void testForeignDataLoad() {
        ITestI instance = TestUtils.create("test/rules/data/ForeignDataLoadTest.xls", ITestI.class);

        Type2[] data = instance.getData2();
        assertEquals(4, data.length);

        assertEquals(1, data[0].getTypes().length);
        assertNull(data[1].getTypes());
        assertEquals(2, data[2].getTypes().length);
        assertEquals(1, data[3].getTypes().length);
    }

    public interface ITestI {
        Type2[] getData2();
    }
}
