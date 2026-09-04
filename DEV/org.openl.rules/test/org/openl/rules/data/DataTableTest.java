package org.openl.rules.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.openl.message.OpenLMessage;
import org.openl.rules.BaseOpenlBuilderHelper;

/**
 * @author DLiauchuk
 */
class DataTableTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/Tutorial_2_Test.xls";

    public DataTableTest() {
        super(SRC);
    }

    @Test
    void testSimpleStringArray() {
        final var tableName = "Data String simpleStringArray";
        var resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        var member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        var stringData = (String[]) member.getTable().getDataArray();
        assertEquals(5, stringData.length);
        var dataList = new ArrayList<String>(Arrays.asList(stringData));
        assertTrue(dataList.contains("StringValue1"));
        assertTrue(dataList.contains("StringValue2"));
        assertTrue(dataList.contains("StringValue3"));
        assertTrue(dataList.contains("StringValue4"));
        assertTrue(dataList.contains("StringValue5"));
    }

    @Test
    void testTypeWithArrayColumns() {
        final var tableName = "Data TypeWithArray testTypeWithArrayColumns";
        var resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        var member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        var typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertEquals(4, typeWitharray[0].getIntArray().length);
        var dataList = new ArrayList<Integer>();
        for (var i = 0; i < typeWitharray[0].getIntArray().length; i++) {
            dataList.add(typeWitharray[0].getIntArray()[i]);
        }
        assertTrue(dataList.contains(111));
        assertTrue(dataList.contains(23));
        assertTrue(dataList.contains(5));
        assertTrue(dataList.contains(67));
    }

    @Test
    void testTypeWithArrayRows() {
        var tableName = "Data TypeWithArray testTypeWithArrayRows";
        var resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        var member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        var typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertEquals(5, typeWitharray[0].getIntArray().length);
        var dataList = new ArrayList<Integer>();
        for (var i = 0; i < typeWitharray[0].getIntArray().length; i++) {
            dataList.add(typeWitharray[0].getIntArray()[i]);
        }
        assertTrue(dataList.contains(12));
        assertTrue(dataList.contains(13));
        assertTrue(dataList.contains(14));
        assertTrue(dataList.contains(15));
        assertTrue(dataList.contains(16));
    }

    @Test
    void testTypeWithArrayRowsOneElement() {
        var tableName = "Data TypeWithArray testTypeWithArrayRowsOneElement";
        var resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        var member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        var typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertEquals(1, typeWitharray[0].getIntArray().length);
        var dataList = new ArrayList<Integer>();
        for (var i = 0; i < typeWitharray[0].getIntArray().length; i++) {
            dataList.add(typeWitharray[0].getIntArray()[i]);
        }
        assertTrue(dataList.contains(12));
    }

    @Test
    void testCommaSeparated() {
        final var tableName = "Data TypeWithArray testCommaSeparated";
        var resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        var member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        var typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertEquals(5, typeWitharray[0].getIntArray().length);
        var dataList = new ArrayList<Integer>();
        for (var i = 0; i < typeWitharray[0].getIntArray().length; i++) {
            dataList.add(typeWitharray[0].getIntArray()[i]);
        }
        assertTrue(dataList.contains(1));
        assertTrue(dataList.contains(56));
        assertTrue(dataList.contains(78));
        assertTrue(dataList.contains(45));
        assertTrue(dataList.contains(99));
    }

    @Test
    void testStringArray() {
        var tableName = "Data TypeWithArray testStringArray";
        var resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        var member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        var typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertEquals(2, typeWitharray[0].getStringArray().length);
        var dataList = new ArrayList<String>(Arrays.asList(typeWitharray[0].getStringArray()));
        assertTrue(dataList.contains("Hello Denis! My name is vova."));
        assertTrue(dataList.contains("Yeah you are right."));
    }

    @Test
    void testStringArrayWithEscaper() {
        var tableName = "Data TypeWithArray testStringArrayWithEscaper";
        var resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        var member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        var typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertEquals(4, typeWitharray[0].getStringArray().length);
        var dataList = new ArrayList<String>(Arrays.asList(typeWitharray[0].getStringArray()));
        assertTrue(dataList.contains("One"));
        assertTrue(dataList.contains("two"));
        assertTrue(dataList.contains("three,continue this"));
        assertTrue(dataList.contains("four"));
    }

    @Test
    void testClass() {
        var tableName = "Data TypeWithArray testClassLoading";
        var resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        var member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        var typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertEquals(4, typeWitharray[0].getStringArray().length);
        var dataList = new ArrayList<String>(Arrays.asList(typeWitharray[0].getStringArray()));
        assertTrue(dataList.contains("One"));
        assertTrue(dataList.contains("two"));
        assertTrue(dataList.contains("three,continue this"));
        assertTrue(dataList.contains("four"));
    }

    @Test
    void testDataTableWithClass() {
        // TODO: Fix it. There should be no error messages
        Collection<OpenLMessage> messages = getCompiledOpenClass().getAllMessages();
        assertEquals(1, messages.size());
        assertEquals("Cannot parse cell value '1 < 2'. Expected value of type 'ClassForStringConstructorLoadingTests'.",
                messages.iterator().next().getSummary());
    }
}
