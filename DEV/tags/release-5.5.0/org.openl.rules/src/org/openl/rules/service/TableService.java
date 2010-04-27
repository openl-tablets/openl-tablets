package org.openl.rules.service;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.XlsSheetGridModel;

public interface TableService {

    public void removeTable(IGridTable table) throws TableServiceException;

    public IGridRegion copyTable(IGridTable table, XlsSheetGridModel destSheetModel) throws TableServiceException;

    /**
     * Copies table into specified region.
     * 
     * @param table Table to copy.
     * @param destSheetModel Destination sheet for copied table.
     * @param destRegion Destination region for copied table inside the sheet.
     * @throws TableServiceException
     */
    public void copyTableTo(IGridTable table, XlsSheetGridModel destSheetModel, IGridRegion destRegion)
            throws TableServiceException;

    public IGridRegion moveTable(IGridTable table, XlsSheetGridModel destSheetModel) throws TableServiceException;

    /**
     * Moves table into specified region.
     * 
     * @param table Table to move.
     * @param destSheetModel Destination sheet for moved table.
     * @param destRegion Destination region for moved table inside the sheet.
     * @throws TableServiceException
     */
    public void moveTableTo(IGridTable table, XlsSheetGridModel destSheetModel, IGridRegion destRegion)
            throws TableServiceException;

}
