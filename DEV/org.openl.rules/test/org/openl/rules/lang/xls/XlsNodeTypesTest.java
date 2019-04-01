package org.openl.rules.lang.xls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class XlsNodeTypesTest {
    @Test
    public void testGetConstant() {
        assertEquals(XlsNodeTypes.WORKBOOK, XlsNodeTypes.getEnumByValue("Workbook"));
        assertEquals(XlsNodeTypes.WORKSHEET, XlsNodeTypes.getEnumByValue("Worksheet"));
        assertEquals(XlsNodeTypes.TABLE, XlsNodeTypes.getEnumByValue("Table"));
        assertEquals(XlsNodeTypes.CELL, XlsNodeTypes.getEnumByValue("Cell"));

        assertEquals(XlsNodeTypes.XLS_MODULE, XlsNodeTypes.getEnumByValue("xls.module"));
        assertEquals(XlsNodeTypes.XLS_WORKBOOK, XlsNodeTypes.getEnumByValue("xls.workbook"));
        assertEquals(XlsNodeTypes.XLS_WORKSHEET, XlsNodeTypes.getEnumByValue("xls.worksheet"));

        assertEquals(XlsNodeTypes.XLS_DT, XlsNodeTypes.getEnumByValue("xls.dt"));
        assertEquals(XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.getEnumByValue("xls.spreadsheet"));
        assertEquals(XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.getEnumByValue("xls.tbasic"));
        assertEquals(XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.getEnumByValue("xls.columnmatch"));
        assertEquals(XlsNodeTypes.XLS_METHOD, XlsNodeTypes.getEnumByValue("xls.method"));
        assertEquals(XlsNodeTypes.XLS_TEST_METHOD, XlsNodeTypes.getEnumByValue("xls.test.method"));
        assertEquals(XlsNodeTypes.XLS_RUN_METHOD, XlsNodeTypes.getEnumByValue("xls.run.method"));

        assertEquals(XlsNodeTypes.XLS_DATA, XlsNodeTypes.getEnumByValue("xls.data"));
        assertEquals(XlsNodeTypes.XLS_DATATYPE, XlsNodeTypes.getEnumByValue("xls.datatype"));
        assertEquals(XlsNodeTypes.XLS_OPENL, XlsNodeTypes.getEnumByValue("xls.openl"));
        assertEquals(XlsNodeTypes.XLS_ENVIRONMENT, XlsNodeTypes.getEnumByValue("xls.environment"));
        assertEquals(XlsNodeTypes.XLS_OTHER, XlsNodeTypes.getEnumByValue("xls.other"));
        assertEquals(XlsNodeTypes.XLS_PROPERTIES, XlsNodeTypes.getEnumByValue("xls.properties"));

        assertNull(XlsNodeTypes.getEnumByValue(null));
        assertNull(XlsNodeTypes.getEnumByValue(""));
        assertNull(XlsNodeTypes.getEnumByValue("abracadabra"));
    }
}
