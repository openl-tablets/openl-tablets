package org.openl.meta;

import static junit.framework.Assert.*;

import org.junit.Test;

public class TestDoubleValue {
    @Test
    public void testEquals() {
        DoubleValue value1 = new DoubleValue(10.2, new ValueMetaInfo("shortName", "fullName", null), "");
        DoubleValue value2 = new DoubleValue(10.2, new ValueMetaInfo("shortName2", "fullName2", null), "");
        assertEquals(value1, value2);
        value2.setMetaInfo(value1.getMetaInfo());
        assertEquals(value1, value2);
    }
}
