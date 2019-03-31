package org.openl.excel.parser.dom;

import org.openl.excel.parser.BaseReaderTest;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.FolderUtils;

public class DOMXlsReaderFromFileTest extends BaseReaderTest {
    @Override
    protected ExcelReader createReader() {
        return new DOMReader(FolderUtils.getResourcesFolder() + "small.xls");
    }
}
