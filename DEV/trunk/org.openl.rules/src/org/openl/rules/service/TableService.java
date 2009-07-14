package org.openl.rules.service;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.XlsSheetGridModel;

public interface TableService {
    
    public void removeTable(IGridTable table) throws TableServiceException;
    
    public IGridRegion copyTable(IGridTable table, XlsSheetGridModel destSheetModel) throws TableServiceException;

    public IGridRegion moveTable(IGridTable table, XlsSheetGridModel destSheetModel) throws TableServiceException;
    
}
