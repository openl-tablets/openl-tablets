package org.openl.rules.data;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.openl.message.OpenLMessage;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 *
 * @author DLiauchuk
 *
 */
public class DataTableTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/Tutorial_2_Test.xls";

    public DataTableTest() {
        super(SRC);
    }

    @Test
    public void testSimpleStringArray() {
        final String tableName = "Data String simpleStringArray";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        DataOpenField member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        String[] stringData = (String[]) member.getTable().getDataArray();
        assertTrue(stringData.length == 5);
        List<String> dataList = new ArrayList<>();
        for (String data : stringData) {
            dataList.add(data);
        }
        assertTrue(dataList.contains("StringValue1"));
        assertTrue(dataList.contains("StringValue2"));
        assertTrue(dataList.contains("StringValue3"));
        assertTrue(dataList.contains("StringValue4"));
        assertTrue(dataList.contains("StringValue5"));
    }

    @Test
    public void testTypeWithArrayColumns() {
        final String tableName = "Data TypeWithArray testTypeWithArrayColumns";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        DataOpenField member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        TypeWithArray[] typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertTrue(typeWitharray[0].getIntArray().length == 4);
        List<Integer> dataList = new ArrayList<>();
        for (int i = 0; i < typeWitharray[0].getIntArray().length; i++) {
            dataList.add(typeWitharray[0].getIntArray()[i]);
        }
        assertTrue(dataList.contains(111));
        assertTrue(dataList.contains(23));
        assertTrue(dataList.contains(5));
        assertTrue(dataList.contains(67));
    }

    @Test
    public void testTypeWithArrayRows() {
        String tableName = "Data TypeWithArray testTypeWithArrayRows";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        DataOpenField member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        TypeWithArray[] typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertTrue(typeWitharray[0].getIntArray().length == 5);
        List<Integer> dataList = new ArrayList<>();
        for (int i = 0; i < typeWitharray[0].getIntArray().length; i++) {
            dataList.add(typeWitharray[0].getIntArray()[i]);
        }
        assertTrue(dataList.contains(12));
        assertTrue(dataList.contains(13));
        assertTrue(dataList.contains(14));
        assertTrue(dataList.contains(15));
        assertTrue(dataList.contains(16));
    }

    @Test
    public void testTypeWithArrayRowsOneElement() {
        String tableName = "Data TypeWithArray testTypeWithArrayRowsOneElement";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        DataOpenField member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        TypeWithArray[] typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertTrue(typeWitharray[0].getIntArray().length == 1);
        List<Integer> dataList = new ArrayList<>();
        for (int i = 0; i < typeWitharray[0].getIntArray().length; i++) {
            dataList.add(typeWitharray[0].getIntArray()[i]);
        }
        assertTrue(dataList.contains(12));
    }

    @Test
    public void testCommaSeparated() {
        final String tableName = "Data TypeWithArray testCommaSeparated";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        DataOpenField member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        TypeWithArray[] typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertTrue(typeWitharray[0].getIntArray().length == 5);
        List<Integer> dataList = new ArrayList<>();
        for (int i = 0; i < typeWitharray[0].getIntArray().length; i++) {
            dataList.add(typeWitharray[0].getIntArray()[i]);
        }
        assertTrue(dataList.contains(1));
        assertTrue(dataList.contains(56));
        assertTrue(dataList.contains(78));
        assertTrue(dataList.contains(45));
        assertTrue(dataList.contains(99));
    }

    @Test
    public void testStringArray() {
        String tableName = "Data TypeWithArray testStringArray";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        DataOpenField member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        TypeWithArray[] typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertTrue(typeWitharray[0].getStringArray().length == 2);
        List<String> dataList = new ArrayList<>();
        for (String token : typeWitharray[0].getStringArray()) {
            dataList.add(token);
        }
        assertTrue(dataList.contains("Hello Denis! My name is vova."));
        assertTrue(dataList.contains("Yeah you are right."));
    }

    @Test
    public void testStringArrayWithEscaper() {
        String tableName = "Data TypeWithArray testStringArrayWithEscaper";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        DataOpenField member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        TypeWithArray[] typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertTrue(typeWitharray[0].getStringArray().length == 4);
        List<String> dataList = new ArrayList<>();
        for (String token : typeWitharray[0].getStringArray()) {
            dataList.add(token);
        }
        assertTrue(dataList.contains("One"));
        assertTrue(dataList.contains("two"));
        assertTrue(dataList.contains("three,continue this"));
        assertTrue(dataList.contains("four"));
    }

    @Test
    public void testClass() {
        String tableName = "Data TypeWithArray testClassLoading";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        DataOpenField member = (DataOpenField) resultTsn.getMember();
        assertNotNull(member);
        TypeWithArray[] typeWitharray = (TypeWithArray[]) member.getTable().getDataArray();
        assertTrue(typeWitharray[0].getStringArray().length == 4);
        List<String> dataList = new ArrayList<>();
        for (String token : typeWitharray[0].getStringArray()) {
            dataList.add(token);
        }
        assertTrue(dataList.contains("One"));
        assertTrue(dataList.contains("two"));
        assertTrue(dataList.contains("three,continue this"));
        assertTrue(dataList.contains("four"));
    }

    @Test
    public void testDataTableWithClass() {
        // TODO: Fix it. There should be no error messages
        Collection<OpenLMessage> messages = getCompiledOpenClass().getMessages();
        assertEquals(1, messages.size());
        assertEquals("Cannot parse cell value '1 < 2'. Expected value of type 'ClassForStringConstructorLoadingTests'.",
            messages.iterator().next().getSummary());
    }
}
