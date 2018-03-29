package org.openl.excel.parser.event;

import java.io.FileInputStream;
import java.io.IOException;

import org.openl.excel.parser.BaseReaderTest;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.FolderUtils;

public class EventInputStreamReaderTest extends BaseReaderTest {
    @Override
    protected ExcelReader createReader() throws IOException {
        return new EventReader(new FileInputStream(FolderUtils.getResourcesFolder() + "small.xls"));
    }
}
