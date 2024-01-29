package org.openl.excel.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ExcelReaderFactoryTest {
    private static final String FOLDER = FolderUtils.getResourcesFolder();

    @Test
    public void compareSmallXlsx() {
        compareDifferentImplementations(FOLDER + "small.xlsx");
    }

    @Test
    public void compareSmallXls() {
        compareDifferentImplementations(FOLDER + "small.xls");
    }

    private void compareDifferentImplementations(String fileName) {
        ExcelReaderFactory sequentialFactory = ExcelReaderFactory.sequentialFactory();
        ExcelReaderFactory fullReadFactory = ExcelReaderFactory.fullReadFactory();

        try (ExcelReader lightReader = sequentialFactory.create(fileName);
                ExcelReader fullReader = fullReadFactory.create(fileName)) {
            List<? extends SheetDescriptor> fullReaderSheets = fullReader.getSheets();
            List<? extends SheetDescriptor> lightReaderSheets = lightReader.getSheets();

            assertEquals(fullReaderSheets.size(), lightReaderSheets.size());

            for (int i = 0; i < fullReaderSheets.size(); i++) {
                SheetDescriptor fullSheet = fullReaderSheets.get(i);
                SheetDescriptor lightSheet = lightReaderSheets.get(i);

                assertEquals(fullSheet.getName(), lightSheet.getName());
                assertArrayEquals(fullReader.getCells(fullSheet),
                    lightReader.getCells(lightSheet),
                    "Cells are not equal for sheet '" + fullSheet.getName() + "'");
            }
        }
    }
}