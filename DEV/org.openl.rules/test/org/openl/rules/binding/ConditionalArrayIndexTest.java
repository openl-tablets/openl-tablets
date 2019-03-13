package org.openl.rules.binding;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;

public class ConditionalArrayIndexTest {

    private static Object instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create("test/rules/binding/ConditionalArrayIndexTest.xlsx");
    }

    @Test
    public void testXPathLikeExpression() throws Exception {
        Object[] drivers = TestUtils.invoke(instance, "getTestDrivers");
        assertEquals(drivers[2], TestUtils.invoke(instance, "driverSelectOne", (Object) drivers));
        Object[] selectManyResult = TestUtils.invoke(instance, "driverSelectMany", (Object) drivers);
        assertArrayEquals(new Object[] { drivers[1], drivers[2] }, selectManyResult);
    }

    @Test
    public void testLiteralExpression() {
        Object[] drivers = TestUtils.invoke(instance, "getTestDrivers");
        assertEquals(drivers[1], TestUtils.invoke(instance, "driverSelectOneLiteral", (Object) drivers));
        Object[] selectManyResult = TestUtils.invoke(instance, "driverSelectManyLiteral", (Object) drivers);
        assertArrayEquals(new Object[] { drivers[1], drivers[2] }, selectManyResult);
    }

    @Test
    public void testSpreadsheetExpression() {
        Object[] drivers = TestUtils.invoke(instance, "getTestDrivers");
        assertEquals(drivers[1], TestUtils.invoke(instance, "checkSpreadsheet1", drivers, 20));
        assertEquals(drivers[0], TestUtils.invoke(instance, "checkSpreadsheet1", drivers, 40));

        assertEquals(drivers[1], TestUtils.invoke(instance, "checkSpreadsheet2", drivers, 1));
        assertEquals(drivers[2], TestUtils.invoke(instance, "checkSpreadsheet2", drivers, 2));
        assertEquals(drivers[0], TestUtils.invoke(instance, "checkSpreadsheet2", drivers, 0));
    }

}
