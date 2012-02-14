package org.openl.rules.binding;

import java.lang.reflect.Array;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import org.openl.binding.impl.BindHelper;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
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
}
