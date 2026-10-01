package org.openl.excel.parser.sax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.openl.excel.parser.BaseReaderTest;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.FolderUtils;

class SAXReaderFromFileTest extends BaseReaderTest {
    @Override
    protected ExcelReader createReader() {
        return new SAXReader(FolderUtils.getResourcesFolder() + "small.xlsx");
    }

    @Test
    void getSheetRelationIds() {
        var sheets = reader.getSheets().stream().map(SAXSheetDescriptor.class::cast).toList();

        assertEquals(4, sheets.size());

        assertNotNull(sheets.getFirst().getRelationId());
        assertNotNull(sheets.get(1).getRelationId());
        assertNotNull(sheets.get(2).getRelationId());
    }

}
