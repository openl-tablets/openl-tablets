package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class DTInvokeTest {

    @Test
    public void testInvoking() {
        Object instance = TestUtils.create("test/rules/dt/DTInvokeTest.xls");
        assertEquals(new Double(1), TestUtils.invoke(instance, "getILFactor", "Comp", "PA"));

        assertEquals(new Double(2), TestUtils.invoke(instance, "getILFactor", "Coll", "PA"));

        assertEquals(new Double(3), TestUtils.invoke(instance, "getILFactor", "Comp", "MH"));

        assertEquals(new Double(4), TestUtils.invoke(instance, "getILFactor", "Comp", "TR"));

        assertEquals(new Double(5), TestUtils.invoke(instance, "getILFactor", "Any", "GH"));
    }
}
