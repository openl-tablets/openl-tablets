package org.openl.excel.parser.sax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.excel.parser.BaseReaderTest;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.FolderUtils;

public class SAXReaderFromFileTest extends BaseReaderTest {
    @Override
    protected ExcelReader createReader() {
        return new SAXReader(FolderUtils.getResourcesFolder() + "small.xlsx");
    }

    @Test
    public void getSheetRelationIds() {
        SAXReader saxReader = (SAXReader) reader;
        List<SAXSheetDescriptor> sheets = saxReader.getSheets();

        assertEquals(4, sheets.size());

        assertNotNull(sheets.get(0).getRelationId());
        assertNotNull(sheets.get(1).getRelationId());
        assertNotNull(sheets.get(2).getRelationId());
    }

}