package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class DTInvokeTest {

    @Test
    public void testInvoking() {
        Object instance = TestUtils.create("test/rules/dt/DTInvokeTest.xls");
        assertEquals(Double.valueOf(1), TestUtils.invoke(instance, "getILFactor", "Comp", "PA"));

        assertEquals(Double.valueOf(2), TestUtils.invoke(instance, "getILFactor", "Coll", "PA"));

        assertEquals(Double.valueOf(3), TestUtils.invoke(instance, "getILFactor", "Comp", "MH"));

        assertEquals(Double.valueOf(4), TestUtils.invoke(instance, "getILFactor", "Comp", "TR"));

        assertEquals(Double.valueOf(5), TestUtils.invoke(instance, "getILFactor", "Any", "GH"));
    }
}
