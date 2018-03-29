package org.openl.excel.parser;

public interface SheetDescriptor {
    /**
     * Get sheet name
     * @return sheet name
     */
    String getName();

    /**
     * Get the first row on physical sheet.
     * Is available after sheet cells are parsed.
     */
    int getFirstRowNum();

    /**
     * Get the first column on physical sheet.
     * Is available after sheet cells are parsed.
     */
    int getFirstColNum();
}
