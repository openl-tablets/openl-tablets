package org.openl.excel.parser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

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

    @Test
    @Ignore("Comparing big xlsx is a long running test. Temporarily ignored.")
    public void compareBigXlsx() {
        // If want test some file, give it the name "big.xlsx", put it in the folder FOLDER and remove @Ignore.
        String fileName = FOLDER + "big.xlsx";
        assumeTrue(new File(fileName).exists());

        compareDifferentImplementations(fileName);
    }

    private void compareDifferentImplementations(String fileName) {
        ExcelReaderFactory sequentialFactory = ExcelReaderFactory.sequentialFactory();
        ExcelReaderFactory fullReadFactory = ExcelReaderFactory.fullReadFactory();

        try (ExcelReader lightReader = sequentialFactory.create(fileName);
                ExcelReader fullReader = fullReadFactory.create(fileName)){
            List<? extends SheetDescriptor> fullReaderSheets = fullReader.getSheets();
            List<? extends SheetDescriptor> lightReaderSheets = lightReader.getSheets();

            assertEquals(fullReaderSheets.size(), lightReaderSheets.size());

            for (int i = 0; i < fullReaderSheets.size(); i++) {
                SheetDescriptor fullSheet = fullReaderSheets.get(i);
                SheetDescriptor lightSheet = lightReaderSheets.get(i);

                assertEquals(fullSheet.getName(), lightSheet.getName());
                assertArrayEquals("Cells aren't equal for sheet '" + fullSheet.getName() + "'", fullReader.getCells(fullSheet), lightReader.getCells(lightSheet));
            }
        }
    }
}