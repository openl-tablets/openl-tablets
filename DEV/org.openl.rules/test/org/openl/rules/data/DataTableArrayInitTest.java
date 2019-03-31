package org.openl.rules.data;

import static org.junit.Assert.*;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
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

    @Before
    public void before() {
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "true");
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

    private Object getP(Object obj) throws NoSuchFieldException,
                                    SecurityException,
                                    IllegalArgumentException,
                                    IllegalAccessException {
        Field field = obj.getClass().getDeclaredField("p");
        field.setAccessible(true);
        Object res = field.get(obj);
        field.setAccessible(false);
        return res;
    }

    private Object[] getVehicles(Object obj) throws NoSuchFieldException,
                                             SecurityException,
                                             IllegalArgumentException,
                                             IllegalAccessException {
        Field field = obj.getClass().getDeclaredField("vehicles");
        field.setAccessible(true);
        Object[] res = (Object[]) field.get(obj);
        field.setAccessible(false);
        return res;
    }

    private String getModel(Object obj) throws NoSuchFieldException,
                                        SecurityException,
                                        IllegalArgumentException,
                                        IllegalAccessException {
        Field field = obj.getClass().getDeclaredField("model");
        field.setAccessible(true);
        String res = (String) field.get(obj);
        field.setAccessible(false);
        return res;
    }

    private Object[] getAddressArry(Object obj) throws NoSuchFieldException,
                                                SecurityException,
                                                IllegalArgumentException,
                                                IllegalAccessException {
        Field field = obj.getClass().getDeclaredField("addressArry");
        field.setAccessible(true);
        Object[] res = (Object[]) field.get(obj);
        field.setAccessible(false);
        return res;
    }

    private int getZip(Object obj) throws NoSuchFieldException,
                                   SecurityException,
                                   IllegalArgumentException,
                                   IllegalAccessException {
        Field field = obj.getClass().getDeclaredField("zip");
        field.setAccessible(true);
        int res = Integer.parseInt(field.get(obj).toString());
        field.setAccessible(false);
        return res;
    }
}
