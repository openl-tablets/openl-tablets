package org.openl.rules.table.actions;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.rules.helpers.INumberRange;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;

/**
 * @author snshor
 */
public class UndoableSetValueAction extends AUndoableCellAction {

    private Object newValue;

    public UndoableSetValueAction(int col, int row, Object value, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.newValue = value;
    }

    @Override
    public void doAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();

        ICell cell = grid.getCell(getCol(), getRow());
        setPrevValue(cell.getObjectValue());
        setPrevFormula(cell.getFormula());
        setPrevMetaInfo(metaInfoWriter.getMetaInfo(getRow(), getCol()));

        grid.setCellValue(getCol(), getRow(), newValue);

        CellMetaInfo newMetaInfo = getNewMetaInfo(newValue);
        if (newMetaInfo != null) {
            metaInfoWriter.setMetaInfo(getRow(), getCol(), newMetaInfo);
        }
    }

    @Override
    public void undoAction(IGridTable table) {
        IWritableGrid grid = (IWritableGrid) table.getGrid();
        if (StringUtils.isNotBlank(getPrevFormula())) {
            grid.setCellFormula(getCol(), getRow(), getPrevFormula());
        } else {
            grid.setCellValue(getCol(), getRow(), getPrevValue());
        }
        metaInfoWriter.setMetaInfo(getRow(), getCol(), getPrevMetaInfo());
    }

    private CellMetaInfo getNewMetaInfo(Object value) {
        if (value == null) {
            return null;
        }
        CellMetaInfo prevMetaInfo = getPrevMetaInfo();
        IOpenClass newType = JavaOpenClass.getOpenClass(value.getClass());
        if (prevMetaInfo != null && prevMetaInfo.getDataType() != null && prevMetaInfo.getDataType().equals(newType)) {
            return removeNodeUsage(prevMetaInfo);
        }

        IOpenClass dataType = prevMetaInfo == null ? null : prevMetaInfo.getDataType();
        if (dataType != null) {
            IDomain<?> domain = dataType.getDomain();
            boolean keepOldMetaInfo = domain instanceof EnumDomain<?> ||
                    ClassUtils.isAssignable(dataType.getInstanceClass(), INumberRange.class);
            if (keepOldMetaInfo) {
                // Don't change meta info
                return removeNodeUsage(prevMetaInfo);
            }
        }

        boolean multiValue = false;
        if (newType.getAggregateInfo().isAggregate(newType)) {
            newType = newType.getAggregateInfo().getComponentType(newType);
            multiValue = true;
        }

        return new CellMetaInfo(newType, multiValue);
    }

    /**
     * Remove NodeUsage for a new value because it can contain another string so NodeUsage will be incorrect.
     *
     * @param metaInfo old meta info
     * @return new meta info
     */
    private CellMetaInfo removeNodeUsage(CellMetaInfo metaInfo) {
        return new CellMetaInfo(metaInfo.getDataType(), metaInfo.isMultiValue());
    }

}
