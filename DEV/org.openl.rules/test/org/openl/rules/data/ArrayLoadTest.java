package org.openl.rules.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class ArrayLoadTest {

    private static final String SRC = "test/rules/data/MultiRowArrayLoadTest.xls";

    @Test
    public void testMultiRowArrayLoad() {
        ITestI instance = TestUtils.create(SRC, ITestI.class);

        Customer[] customers = instance.getCustomers();
        assertEquals(2, customers.length);

        assertEquals(5, customers[0].getProblems().length);
        assertNull(customers[0].getProblems()[0]);
        assertNull(customers[0].getProblems()[1]);
        assertNull(customers[0].getProblems()[2]);
        assertNull(customers[0].getProblems()[3]);
        assertEquals("Bill Pay - Overdraft", customers[0].getProblems()[4]);

        assertEquals(1, customers[1].getProblems().length);
        assertEquals("Bill Pay - Overdraft", customers[1].getProblems()[0]);

        assertEquals(5, customers[0].getProducts().length);
        assertEquals("Checking Account", customers[0].getProducts()[0]);
        assertNull(customers[0].getProducts()[1]);
        assertNull(customers[0].getProducts()[2]);
        assertNull(customers[0].getProducts()[3]);
        assertEquals("Saving Account", customers[0].getProducts()[4]);

        assertEquals(3, customers[1].getProducts().length);
        assertNull(customers[1].getProducts()[0]);
        assertNull(customers[1].getProducts()[1]);
        assertEquals("Checking Account", customers[1].getProducts()[2]);
    }

    public interface ITestI {
        Customer[] getCustomers();
    }
}
