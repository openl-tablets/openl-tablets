package org.openl.rules.binding;

import java.lang.reflect.Array;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.openl.binding.impl.BindHelper;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;

import static org.junit.Assert.*;

public class ConditionalArrayIndexTest extends BaseOpenlBuilderHelper {
    private static String SRC = "test/rules/binding/ConditionalArrayIndexTest.xlsx";

    public ConditionalArrayIndexTest() {
        super(SRC);
        
    }

    @Test
    public void testXPathLikeExpression() throws Exception {
        IOpenField driverField = getField("testDrivers");
        Object drivers = getFieldValue("testDrivers");
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
        IOpenField driverField = getField("testDrivers");
        Object drivers = getFieldValue("testDrivers");
        assertEquals(
                invokeMethod("driverSelectOneLiteral", new IOpenClass[] { driverField.getType() },
                        new Object[] { drivers }), Array.get(drivers, 1));
        Object[] selectManyResult = (Object[]) invokeMethod("driverSelectManyLiteral",
                new IOpenClass[] { driverField.getType() }, new Object[] { drivers });
        assertTrue(ArrayUtils.contains(selectManyResult, Array.get(drivers, 1)));
        assertTrue(ArrayUtils.contains(selectManyResult, Array.get(drivers, 2)));
    }

    public boolean containsErrorWithMessage(TableSyntaxNode tsn, String message){
        for(SyntaxNodeException error : tsn.getErrors()){
            if(error.getMessage().equals(message)){
                return true;
            }
        }
        return false;
    }
    
    @Test
    public void testWrongExpressions() {
        TableSyntaxNode tableWithError = findTable("Method Driver[] errorSelect(Driver[] arrayOfDrivers)");
        TableSyntaxNode tableWithError2 = findTable("Method Driver[] errorSelectLiteral(Driver[] arrayOfDrivers)");
        assertTrue(containsErrorWithMessage(tableWithError, BindHelper.CONDITION_TYPE_MESSAGE));
        assertTrue(containsErrorWithMessage(tableWithError2, BindHelper.CONDITION_TYPE_MESSAGE));
    }

    @Test
    public void testSpreadsheetExpression() throws Exception {
        IOpenField driverField = getField("testDrivers");
        Object drivers = getFieldValue("testDrivers");
        assertEquals(
                invokeMethod("checkSpreadsheet1", new IOpenClass[] { driverField.getType(), JavaOpenClass.INT}, new Object[] { drivers, 20 }),
                Array.get(drivers, 1));

        assertEquals(
                invokeMethod("checkSpreadsheet1", new IOpenClass[] { driverField.getType(), JavaOpenClass.INT}, new Object[] { drivers, 40 }),
                Array.get(drivers, 0));
    
        assertEquals(
                invokeMethod("checkSpreadsheet2", new IOpenClass[] { driverField.getType(), JavaOpenClass.INT}, new Object[] { drivers, 1 }),
                Array.get(drivers, 1));

        assertEquals(
                invokeMethod("checkSpreadsheet2", new IOpenClass[] { driverField.getType(), JavaOpenClass.INT}, new Object[] { drivers, 2 }),
                Array.get(drivers, 2));

        assertEquals(
                invokeMethod("checkSpreadsheet2", new IOpenClass[] { driverField.getType(), JavaOpenClass.INT}, new Object[] { drivers, 0 }),
                Array.get(drivers, 0));
    }


}
