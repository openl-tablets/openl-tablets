package org.openl.rules.binding;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

public class AutoCastReturnTypeTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/binding/AutoCastReturnTypeMethodsTest.xlsx";

    public AutoCastReturnTypeTest() {
        super(SRC);
    }

    @Test
    public void testInitializationJavaStyle() {
        String[] values = (String[]) invokeMethod("flattenTest");
        Assert.assertArrayEquals(new String[] { "1", "2", "3", "1", "2", "3" }, values);
        Integer[] values2 = (Integer[]) invokeMethod("getValuesTest");
        Assert.assertArrayEquals(new Integer[] { 1, 2, -5, 0 }, values2);
    }
}