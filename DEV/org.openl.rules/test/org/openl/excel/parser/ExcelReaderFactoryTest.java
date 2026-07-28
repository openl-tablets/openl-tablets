package org.openl.excel.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ExcelReaderFactoryTest {
    private static final String FOLDER = FolderUtils.getResourcesFolder();

    @Test
    void compareSmallXlsx() {
        compareDifferentImplementations(FOLDER + "small.xlsx");
    }

    @Test
    void compareSmallXls() {
        compareDifferentImplementations(FOLDER + "small.xls");
    }

    private void compareDifferentImplementations(String fileName) {
        ExcelReaderFactory sequentialFactory = ExcelReaderFactory.sequentialFactory();
        ExcelReaderFactory fullReadFactory = ExcelReaderFactory.fullReadFactory();

        try (var lightReader = sequentialFactory.create(fileName);
             var fullReader = fullReadFactory.create(fileName)) {
            List<? extends SheetDescriptor> fullReaderSheets = fullReader.getSheets();
            List<? extends SheetDescriptor> lightReaderSheets = lightReader.getSheets();

            assertEquals(fullReaderSheets.size(), lightReaderSheets.size());

            for (var i = 0; i < fullReaderSheets.size(); i++) {
                var fullSheet = fullReaderSheets.get(i);
                var lightSheet = lightReaderSheets.get(i);

                assertEquals(fullSheet.getName(), lightSheet.getName());
                assertArrayEquals(fullReader.getCells(fullSheet),
                        lightReader.getCells(lightSheet),
                        "Cells are not equal for sheet '" + fullSheet.getName() + "'");
            }
        }
    }
}
