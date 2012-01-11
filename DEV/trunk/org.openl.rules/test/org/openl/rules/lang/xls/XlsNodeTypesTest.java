package org.openl.rules.lang.xls;

import static org.junit.Assert.*;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class XlsNodeTypesTest {
    @Test
    public void testGetConstant() {
        assertEquals(XlsNodeTypes.WORKBOOK, XlsNodeTypes.getEnumConstant("Workbook"));
        assertEquals(XlsNodeTypes.WORKSHEET, XlsNodeTypes.getEnumConstant("Worksheet"));
        assertEquals(XlsNodeTypes.TABLE, XlsNodeTypes.getEnumConstant("Table"));        
        assertEquals(XlsNodeTypes.CELL, XlsNodeTypes.getEnumConstant("Cell"));
        
        assertEquals(XlsNodeTypes.XLS_MODULE, XlsNodeTypes.getEnumConstant("xls.module"));
        assertEquals(XlsNodeTypes.XLS_WORKBOOK, XlsNodeTypes.getEnumConstant("xls.workbook"));
        assertEquals(XlsNodeTypes.XLS_WORKSHEET, XlsNodeTypes.getEnumConstant("xls.worksheet"));
        
        assertEquals(XlsNodeTypes.XLS_DT, XlsNodeTypes.getEnumConstant("xls.dt"));
        assertEquals(XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.getEnumConstant("xls.spreadsheet"));
        assertEquals(XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.getEnumConstant("xls.tbasic"));
        assertEquals(XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.getEnumConstant("xls.columnmatch"));
        assertEquals(XlsNodeTypes.XLS_METHOD, XlsNodeTypes.getEnumConstant("xls.method"));
        assertEquals(XlsNodeTypes.XLS_TEST_METHOD, XlsNodeTypes.getEnumConstant("xls.test.method"));        
        assertEquals(XlsNodeTypes.XLS_RUN_METHOD, XlsNodeTypes.getEnumConstant("xls.run.method"));
        
        assertEquals(XlsNodeTypes.XLS_DATA, XlsNodeTypes.getEnumConstant("xls.data"));
        assertEquals(XlsNodeTypes.XLS_DATATYPE, XlsNodeTypes.getEnumConstant("xls.datatype"));
        assertEquals(XlsNodeTypes.XLS_OPENL, XlsNodeTypes.getEnumConstant("xls.openl"));
        assertEquals(XlsNodeTypes.XLS_ENVIRONMENT, XlsNodeTypes.getEnumConstant("xls.environment"));
        assertEquals(XlsNodeTypes.XLS_PERSISTENT, XlsNodeTypes.getEnumConstant("xls.persistent"));
        assertEquals(XlsNodeTypes.XLS_OTHER, XlsNodeTypes.getEnumConstant("xls.other"));        
        assertEquals(XlsNodeTypes.XLS_PROPERTIES, XlsNodeTypes.getEnumConstant("xls.properties"));
        
        assertNull(XlsNodeTypes.getEnumConstant(null));
        assertNull(XlsNodeTypes.getEnumConstant(StringUtils.EMPTY));
        assertNull(XlsNodeTypes.getEnumConstant("abracadabra"));
    }
}
