package org.openl.excel.parser.event;

import org.openl.excel.parser.BaseReaderTest;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.FolderUtils;

public class EventFileReaderTest extends BaseReaderTest {
    @Override
    protected ExcelReader createReader() {
        return new EventReader(FolderUtils.getResourcesFolder() + "small.xls");
    }
}
