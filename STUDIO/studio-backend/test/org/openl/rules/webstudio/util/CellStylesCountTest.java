package org.openl.rules.webstudio.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.table.xls.writers.XlsCellDateWriter;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * Tests below check that there are now exceptions for projects with too many styles count.
 *
 * @author nsamatov.
 */
class CellStylesCountTest {
    /**
     * For more information, see {@link org.apache.poi.hssf.usermodel.HSSFWorkbook#MAX_STYLES}
     */
    private static final short MAX_STYLES = 4030;
    private XlsWorkbookSourceCodeModule wbSrc;

    @BeforeEach
    void setUp() {
        wbSrc = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule("test/rules/TooManyStyles.xls"));
    }

    @Test
    void testXlsSheetGridModel() {
        var grid = new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, wbSrc));

        grid.setCellStyle(0, 0, grid.getCell(1, 1).getStyle());
        assertTrue(wbSrc.getWorkbook().getNumCellStyles() < MAX_STYLES,
                "Styles count should be less than " + MAX_STYLES);
    }

    @Test
    void testTableBuilder() throws Exception {
        var builder = new TableBuilder(new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, wbSrc)));

        builder.beginTable(TableBuilder.PROPERTIES_MIN_WIDTH, TableBuilder.HEADER_HEIGHT + 1);
        builder.writeHeader("Datatype a1 <a2>", null);
        builder.writeProperties(Map.of("name", "value"), null);
        builder.endTable();

        assertTrue(wbSrc.getWorkbook().getNumCellStyles() < MAX_STYLES,
                "Styles count should be less than " + MAX_STYLES);
    }

    @Test
    void testPoiExcelHelper() {
        Cell cellFrom = PoiExcelHelper.getOrCreateCell(0, 0, new XlsSheetSourceCodeModule(0, wbSrc).getSheet());

        PoiExcelHelper.cloneStyleFrom(cellFrom);
        assertTrue(wbSrc.getWorkbook().getNumCellStyles() < MAX_STYLES,
                "Styles count should be less than " + MAX_STYLES);
    }

    @Test
    void testXlsCellDateWriter() {
        var sheetSource = new XlsSheetSourceCodeModule(0, wbSrc);
        var grid = new XlsSheetGridModel(sheetSource);

        var writer = new XlsCellDateWriter(grid);
        writer.setCellToWrite(PoiExcelHelper.getOrCreateCell(0, 0, sheetSource.getSheet()));
        writer.setValueToWrite(new Date());
        writer.writeCellValue();
        assertTrue(wbSrc.getWorkbook().getNumCellStyles() < MAX_STYLES,
                "Styles count should be less than " + MAX_STYLES);
    }

}
