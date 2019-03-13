package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestUtils;

public class DTInvokeTest {

    @Test
    public void testInvoking() {
        Object instance = TestUtils.create("test/rules/dt/DTInvokeTest.xls");
        assertEquals(new DoubleValue(1), TestUtils.invoke(instance, "getILFactor", "Comp", "PA"));

        assertEquals(new DoubleValue(2), TestUtils.invoke(instance, "getILFactor", "Coll", "PA"));

        assertEquals(new DoubleValue(3), TestUtils.invoke(instance, "getILFactor", "Comp", "MH"));

        assertEquals(new DoubleValue(4), TestUtils.invoke(instance, "getILFactor", "Comp", "TR"));

        assertEquals(new DoubleValue(5), TestUtils.invoke(instance, "getILFactor", "Any", "GH"));
    }
}
