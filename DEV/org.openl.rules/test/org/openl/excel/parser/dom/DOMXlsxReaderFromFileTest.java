package org.openl.excel.parser.dom;

import java.io.IOException;

import org.openl.excel.parser.BaseReaderTest;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.FolderUtils;

public class DOMXlsxReaderFromFileTest extends BaseReaderTest {
    @Override
    protected ExcelReader createReader() {
        return new DOMReader(FolderUtils.getResourcesFolder() + "small.xlsx");
    }
}
