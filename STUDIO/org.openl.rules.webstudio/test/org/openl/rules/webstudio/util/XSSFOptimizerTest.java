package org.openl.rules.webstudio.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyle;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyles;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;

public class XSSFOptimizerTest {

    @Test
    public void removeUnusedStyles() throws Exception {
        byte[] savedFile;

        try (FileInputStream inputStream = new FileInputStream("test-resources/XSSFOptimizerTest.xlsx")) {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

            // Check the state before optimization
            CTCellStyles cellStyles = workbook.getStylesSource().getCTStylesheet().getCellStyles();
            assertEquals(3, cellStyles.getCellStyleList().size());
            assertStyleExist("My Custom Style 2", cellStyles.getCellStyleList());

            // Remove unused styles and save the file
            XSSFOptimizer.removeUnusedStyles(workbook);
            savedFile = save(workbook);
        }

        // Read saved file and get style info
        StylesTable stylesSource = new XSSFWorkbook(new ByteArrayInputStream(savedFile)).getStylesSource();
        CTCellStyles cellStyles = stylesSource.getCTStylesheet().getCellStyles();

        // Check that styles are removed
        assertNotNull(cellStyles);
        List<CTCellStyle> styleList = cellStyles.getCellStyleList();
        assertEquals(2, styleList.size());
        assertStyleExist("My Custom Style 1", styleList);
        assertStyleExist("Normal", styleList);

        @SuppressWarnings("unchecked")
        List<CTXf> styleXfs = (List<CTXf>) FieldUtils.readDeclaredField(stylesSource, "styleXfs", true);
        assertNotNull(styleXfs);
        assertEquals(3, styleXfs.size());
    }

    private void assertStyleExist(String name, List<CTCellStyle> styleList) {
        boolean found = false;
        for (CTCellStyle style : styleList) {
            if (name.equals(style.getName())) {
                found = true;
                break;
            }
        }
        assertTrue("Style " + name + " not found", found);
    }

    private byte[] save(XSSFWorkbook workbook) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        return outputStream.toByteArray();
    }
}