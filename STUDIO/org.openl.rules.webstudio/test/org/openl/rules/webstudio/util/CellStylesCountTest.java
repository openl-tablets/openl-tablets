package org.openl.rules.webstudio.util;

import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.DatatypeAliasTableBuilder;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.table.xls.writers.XlsCellDateWriter;
import org.openl.rules.ui.tablewizard.util.CellStyleCreator;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * Tests below check that there are now exceptions for projects with too many styles count.
 *
 * @author nsamatov.
 */
public class CellStylesCountTest {
    /**
     * For more information, see {@link org.apache.poi.hssf.usermodel.HSSFWorkbook#MAX_STYLES}
     */
    private static final short MAX_STYLES = 4030;
    private XlsWorkbookSourceCodeModule wbSrc;

    @Before
    public void setUp() {
        wbSrc = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule("test/rules/TooManyStyles.xls"));
    }

    @Test
    public void testXlsSheetGridModel() {
        XlsSheetGridModel grid = new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, wbSrc));

        grid.setCellStyle(0, 0, grid.getCell(1, 1).getStyle());
        assertTrue("Styles count should be less than " + MAX_STYLES, wbSrc.getWorkbook().getNumCellStyles() < MAX_STYLES);
    }

    @Test
    public void testTableBuilder() throws Exception {
        DatatypeAliasTableBuilder builder = new DatatypeAliasTableBuilder(new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, wbSrc)));

        builder.beginTable(DatatypeAliasTableBuilder.MIN_WIDTH, TableBuilder.HEADER_HEIGHT);
        builder.writeHeader("a1", "a2");
        builder.writeValue("value");
        builder.endTable();

        assertTrue("Styles count should be less than " + MAX_STYLES, wbSrc.getWorkbook().getNumCellStyles() < MAX_STYLES);
    }

    @Test
    public void testPoiExcelHelper() {
        Cell cellFrom = PoiExcelHelper.getOrCreateCell(0, 0, new XlsSheetSourceCodeModule(0, wbSrc).getSheet());

        PoiExcelHelper.cloneStyleFrom(cellFrom);
        assertTrue("Styles count should be less than " + MAX_STYLES, wbSrc.getWorkbook().getNumCellStyles() < MAX_STYLES);
    }

    @Test
    public void testXlsCellDateWriter() {
        XlsSheetSourceCodeModule sheetSource = new XlsSheetSourceCodeModule(0, wbSrc);
        XlsSheetGridModel grid = new XlsSheetGridModel(sheetSource);

        XlsCellDateWriter writer = new XlsCellDateWriter(grid);
        writer.setCellToWrite(PoiExcelHelper.getOrCreateCell(0, 0, sheetSource.getSheet()));
        writer.setValueToWrite(new Date());
        writer.writeCellValue();
        assertTrue("Styles count should be less than " + MAX_STYLES, wbSrc.getWorkbook().getNumCellStyles() < MAX_STYLES);
    }

    @Test
    public void testCellStyleCreator() {
        XlsSheetGridModel grid = new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, wbSrc));

        new CellStyleCreator(grid).getCellStyle(null);
        assertTrue("Styles count should be less than " + MAX_STYLES, wbSrc.getWorkbook().getNumCellStyles() < MAX_STYLES);
    }
}
