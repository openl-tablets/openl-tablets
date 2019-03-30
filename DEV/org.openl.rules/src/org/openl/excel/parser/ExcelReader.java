package org.openl.excel.parser;

import java.util.List;

import org.openl.rules.table.IGridRegion;

public interface ExcelReader extends AutoCloseable {
    /**
     * Get all sheet descriptors
     */
    List<? extends SheetDescriptor> getSheets();

    /**
     * Parse and get all cells from a given sheet
     *
     * @param sheet sheet to parse
     * @return parsed objects with types as in Excel
     */
    Object[][] getCells(SheetDescriptor sheet);

    /**
     * Sometimes we need to convert parsed double value to date.
     * For example a cell contains value 1.25, user sees it in Excel as 1.25 but in OpenL this value has a type Date.
     * It should be converted from double to Date. (There is unit test for such case)
     * We should get this property from workbook and use it in DateUtil.getJavaDate(double, boolean) to convert it correctly.
     *
     * @return The setting for a given workbook
     */
    boolean isUse1904Windowing();

    /**
     * Get styles for a given table
     *
     * @param sheet sheet containing the table
     * @param tableRegion region needed to get styles for a given table
     * @return Cell styles
     */
    TableStyles getTableStyles(SheetDescriptor sheet, IGridRegion tableRegion);

    /**
     * Close ExcelReader and release resources.
     */
    @Override
    void close();
}
