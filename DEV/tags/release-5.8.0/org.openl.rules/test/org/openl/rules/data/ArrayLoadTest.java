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

        assertEquals(5, customers[0].getProblems().length);
        assertEquals(null, customers[0].getProblems()[0]);
        assertEquals(null, customers[0].getProblems()[1]);
        assertEquals(null, customers[0].getProblems()[2]);
        assertEquals(null, customers[0].getProblems()[3]);
        assertEquals("Bill Pay - Overdraft", customers[0].getProblems()[4]);
        
        assertEquals(1, customers[1].getProblems().length);
        assertEquals("Bill Pay - Overdraft", customers[1].getProblems()[0]);

        assertEquals(5, customers[0].getProducts().length);
        assertEquals("Checking Account", customers[0].getProducts()[0]);
        assertEquals(null, customers[0].getProducts()[1]);
        assertEquals(null, customers[0].getProducts()[2]);
        assertEquals(null, customers[0].getProducts()[3]);
        assertEquals("Saving Account", customers[0].getProducts()[4]);
        
        assertEquals(3, customers[1].getProducts().length);
        assertEquals(null, customers[1].getProducts()[0]);
        assertEquals(null, customers[1].getProducts()[1]);
        assertEquals("Checking Account", customers[1].getProducts()[2]);
    }
}
