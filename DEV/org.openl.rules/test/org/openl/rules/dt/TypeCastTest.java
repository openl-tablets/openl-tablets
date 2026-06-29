package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;
import org.openl.rules.testmethod.TestUnitsResults;

class TypeCastTest {

    @Test
    void testOptimazedAlgorithmExecution() {
        ITest instance = TestUtils.create("test/rules/dt/TypeCastTest.xls", ITest.class);

        TestUnitsResults result = instance.ReplaceSumInsuredTest();
        assertEquals(3, result.getNumberOfTestUnits());
        assertEquals(0, result.getNumberOfFailures());
    }

    public interface ITest {
        TestUnitsResults ReplaceSumInsuredTest();
    }
}
