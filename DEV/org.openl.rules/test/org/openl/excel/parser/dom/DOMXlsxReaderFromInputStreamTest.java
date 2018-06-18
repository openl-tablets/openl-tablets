package org.openl.excel.parser.dom;

import java.io.FileInputStream;
import java.io.IOException;

import org.openl.excel.parser.BaseReaderTest;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.FolderUtils;

public class DOMXlsxReaderFromInputStreamTest extends BaseReaderTest {
    @Override
    protected ExcelReader createReader() throws IOException {
        return new DOMReader(new FileInputStream(FolderUtils.getResourcesFolder() + "small.xlsx"));
    }
}
