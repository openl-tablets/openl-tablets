package org.openl.rules.binding;

import java.lang.reflect.Array;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

import static org.junit.Assert.*;

public class ConditionalArrayIndexTest extends BaseOpenlBuilderHelper {
    private static String src = "test/rules/binding/ConditionalArrayIndexTest.xlsx";

    public ConditionalArrayIndexTest() {
        super(src);
    }

    @Test
    public void testXPathLikeExpression() throws Exception {
        IOpenField driverField = getJavaWrapper().getOpenClass().getField("testDrivers");
        Object drivers = driverField.get(getJavaWrapper().newInstance(), getJavaWrapper().getEnv());
        assertEquals(
                invokeMethod("driverSelectOne", new IOpenClass[] { driverField.getType() }, new Object[] { drivers }),
                Array.get(drivers, 2));
        Object[] selectManyResult = (Object[]) invokeMethod("driverSelectMany",
                new IOpenClass[] { driverField.getType() }, new Object[] { drivers });
        assertTrue(ArrayUtils.contains(selectManyResult, Array.get(drivers, 1)));
        assertTrue(ArrayUtils.contains(selectManyResult, Array.get(drivers, 2)));
    }

    @Test
    public void testLiteralExpression() {
        IOpenField driverField = getJavaWrapper().getOpenClass().getField("testDrivers");
        Object drivers = driverField.get(getJavaWrapper().newInstance(), getJavaWrapper().getEnv());
        assertEquals(
                invokeMethod("driverSelectOneLiteral", new IOpenClass[] { driverField.getType() },
                        new Object[] { drivers }), Array.get(drivers, 1));
        Object[] selectManyResult = (Object[]) invokeMethod("driverSelectManyLiteral",
                new IOpenClass[] { driverField.getType() }, new Object[] { drivers });
        assertTrue(ArrayUtils.contains(selectManyResult, Array.get(drivers, 1)));
        assertTrue(ArrayUtils.contains(selectManyResult, Array.get(drivers, 2)));
    }

}
