package org.openl.rules.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/*
 * @author PTarasevich
 */

public class DataTableArrayInitTest extends BaseOpenlBuilderHelper {
    private static final String FILE_NAME = "test/rules/testmethod/TestDataAccessFieldTest.xlsx";

    public DataTableArrayInitTest() {
        super(FILE_NAME);
    }

    @Test
    public void testTypeWithArrayColumns() {
        String tableName = "Data TestHelperDataBean_v10 testArray";
        TableSyntaxNode resultTsn = findTable(tableName);
        if (resultTsn != null) {
            DataOpenField member = (DataOpenField) resultTsn.getMember();

            assertNotNull(member);

            Object[] typeWitharray = (Object[]) member.getTable().getDataArray();

            assertEquals(15, typeWitharray.length);
            try {
                assertEquals(3, getAddressArry(typeWitharray[3]).length);
                assertNull(getAddressArry(typeWitharray[12])[1]);
                assertEquals(37, getZip(getAddressArry(typeWitharray[12])[0]));
                assertEquals(51, getZip(getAddressArry(typeWitharray[12])[2]));
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        } else {
            fail();
        }
    }

    @Test
    public void testTypeWithArray2Columns() {
        String tableName = "Data TestHelperDataBean_v10 testArray2";
        TableSyntaxNode resultTsn = findTable(tableName);
        if (resultTsn != null) {
            DataOpenField member = (DataOpenField) resultTsn.getMember();

            assertNotNull(member);

            Object[] typeWitharray = (Object[]) member.getTable().getDataArray();

            assertEquals(15, typeWitharray.length);
            try {
                assertEquals(3, getVehicles(getP(typeWitharray[3])).length);
                assertEquals("37", getModel(getVehicles(getP(typeWitharray[12]))[2]));
                assertEquals("51", getModel(getVehicles(getP(typeWitharray[12]))[1]));
                assertNull(getVehicles(getP(typeWitharray[12]))[0]);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        } else {
            fail();
        }
    }

    private Object getP(Object obj) throws Exception {
        return invokeGetter(obj, "getP");
    }

    private Object[] getVehicles(Object obj) throws Exception {
        return invokeGetter(obj, "getVehicles");
    }

    private String getModel(Object obj) throws Exception {
        return invokeGetter(obj, "getModel");
    }

    private Object[] getAddressArry(Object obj) throws Exception {
        return invokeGetter(obj, "getAddressArry");
    }

    private int getZip(Object obj) throws Exception {
        return invokeGetter(obj, "getZip");
    }

    private static <T> T invokeGetter(Object obj, String methodName) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return (T) obj.getClass().getMethod(methodName).invoke(obj);
    }
}
