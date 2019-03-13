package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class DTReturnMultiVariablesTest {

    private static final String SRC = "test/rules/dt/DTReturnMultiVariablesTest.xls";

    @Test
    public void testRightNumberOfParameters() {
        assertEquals("15secondDomainsecondText", TestUtils.invoke(SRC, "validateAttribute", "Driver", "denis"));
    }
}
