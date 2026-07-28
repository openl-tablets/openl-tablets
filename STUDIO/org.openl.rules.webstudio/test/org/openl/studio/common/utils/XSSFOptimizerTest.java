package org.openl.studio.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyle;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;

class XSSFOptimizerTest {

    @Test
    void removeUnusedStyles() throws Exception {
        byte[] savedFile;

        try (var inputStream = new FileInputStream("test-resources/XSSFOptimizerTest.xlsx")) {
            var workbook = new XSSFWorkbook(inputStream);

            // Check the state before optimization
            var cellStyles = workbook.getStylesSource().getCTStylesheet().getCellStyles();
            assertEquals(3, cellStyles.getCellStyleList().size());
            assertStyleExist("My Custom Style 2", cellStyles.getCellStyleList());

            // Remove unused styles and save the file
            XSSFOptimizer.removeUnusedStyles(workbook);
            savedFile = save(workbook);
        }

        // Read saved file and get style info
        var stylesSource = new XSSFWorkbook(new ByteArrayInputStream(savedFile)).getStylesSource();
        var cellStyles = stylesSource.getCTStylesheet().getCellStyles();

        // Check that styles are removed
        assertNotNull(cellStyles);
        List<CTCellStyle> styleList = cellStyles.getCellStyleList();
        assertEquals(2, styleList.size());
        assertStyleExist("My Custom Style 1", styleList);
        assertStyleExist("Normal", styleList);

        @SuppressWarnings("unchecked")
        var styleXfs = (List<CTXf>) FieldUtils.readDeclaredField(stylesSource, "styleXfs", true);
        assertNotNull(styleXfs);
        assertEquals(3, styleXfs.size());
    }

    private void assertStyleExist(String name, List<CTCellStyle> styleList) {
        var found = false;
        for (CTCellStyle style : styleList) {
            if (name.equals(style.getName())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Style '" + name + "' is not found.");
    }

    private byte[] save(XSSFWorkbook workbook) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        return outputStream.toByteArray();
    }
}
