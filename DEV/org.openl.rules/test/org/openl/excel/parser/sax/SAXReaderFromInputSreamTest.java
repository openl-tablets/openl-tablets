package org.openl.excel.parser.sax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.openl.excel.parser.BaseReaderTest;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.FolderUtils;

public class SAXReaderFromInputSreamTest extends BaseReaderTest {
    @Override
    protected ExcelReader createReader() throws IOException {
        return new SAXReader(new FileInputStream(FolderUtils.getResourcesFolder() + "small.xlsx"));
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