package org.openl.rules.data;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;

public class ArrayLoadTest {

    public interface ITestI {
        Customer[] getCustomers();
    }

    @Test
    public void testMultiRowArrayLoad() {
        File xlsFile = new File("test/rules/data/MultiRowArrayLoadTest.xls");
        TestHelper<ITestI> testHelper;
        testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);

        ITestI instance = testHelper.getInstance();

        Customer[] customers = instance.getCustomers();
        assertEquals(2, customers.length);

        assertEquals(1, customers[0].getProblems().length);
        assertEquals(2, customers[0].getProducts().length);
        assertEquals(1, customers[1].getProblems().length);
        assertEquals(1, customers[1].getProducts().length);
    }
}
