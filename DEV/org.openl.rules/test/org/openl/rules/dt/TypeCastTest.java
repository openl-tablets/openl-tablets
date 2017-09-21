package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.testmethod.TestUnitsResults;

public class TypeCastTest {

    public interface ITest {
        TestUnitsResults ReplaceSumInsuredTest();
    }

    @Test
    public void testOptimazedAlgorithmExecution() {

        File xlsFile = new File("test/rules/dt/TypeCastTest.xls");
        TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile, ITest.class);
        
        ITest instance = testHelper.getInstance();
                
        TestUnitsResults result = instance.ReplaceSumInsuredTest();
        assertEquals(3, result.getNumberOfTestUnits());
        assertEquals(0, result.getNumberOfFailures());
    }

}
