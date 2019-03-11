package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;
import org.openl.rules.testmethod.TestUnitsResults;

public class TypeCastTest {

    @Test
    public void testOptimazedAlgorithmExecution() {
        ITest instance = TestUtils.create("test/rules/dt/TypeCastTest.xls", ITest.class);

        TestUnitsResults result = instance.ReplaceSumInsuredTest();
        assertEquals(3, result.getNumberOfTestUnits());
        assertEquals(0, result.getNumberOfFailures());
    }

    public interface ITest {
        TestUnitsResults ReplaceSumInsuredTest();
    }
}
