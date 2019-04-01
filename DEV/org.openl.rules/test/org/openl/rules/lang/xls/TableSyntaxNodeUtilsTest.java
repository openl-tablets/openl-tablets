package org.openl.rules.lang.xls;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TableSyntaxNodeUtilsTest {

    @Test
    public void testStr2Name() {
        assertEquals("dt1", TableSyntaxNodeUtils.str2name("Rules Double dt1(String param1)", XlsNodeTypes.XLS_DT));
        assertEquals("spreadsheet1",
            TableSyntaxNodeUtils.str2name("Spreadsheet SpreadsheetResult spreadsheet1(String param1)",
                XlsNodeTypes.XLS_SPREADSHEET));
        assertEquals("Person", TableSyntaxNodeUtils.str2name("Datatype Person", XlsNodeTypes.XLS_DATATYPE));
        assertEquals("Datatype", TableSyntaxNodeUtils.str2name("Datatype", XlsNodeTypes.XLS_DATATYPE));

        assertEquals("NO NAME", TableSyntaxNodeUtils.str2name(null, XlsNodeTypes.XLS_DATATYPE));
        assertEquals("NO NAME", TableSyntaxNodeUtils.str2name("", XlsNodeTypes.XLS_DATATYPE));

        assertEquals("Rules", TableSyntaxNodeUtils.str2name("Rules", XlsNodeTypes.XLS_DT));

    }
}
