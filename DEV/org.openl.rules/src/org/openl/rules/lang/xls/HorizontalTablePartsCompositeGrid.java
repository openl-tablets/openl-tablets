package org.openl.rules.lang.xls;

import org.openl.rules.table.CompositeCell;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.properties.PropertiesHelper;

class HorizontalTablePartsCompositeGrid extends CompositeGrid {
    public HorizontalTablePartsCompositeGrid(IGridTable[] tables) {
        super(tables, false);
    }

    @Override
    public ICell getCell(int column, int row) {
        Transform t = transform(0, 0);
        if (t == null) {
            return null;
        }
        ICell firstRowCell = t.grid().getCell(t.getCol(), t.getRow());
        if (row < firstRowCell.getHeight()) {
            IGridRegion reg = getRegionContaining(0, 0);
            IGridRegion region;
            if (reg != null) {
                region = new GridRegion(reg.getTop(), reg.getLeft(), reg.getBottom(), reg.getLeft() + getWidth() - 1);
            } else {
                region = new GridRegion(row, column, row, column + getWidth() - 1);
            }
            return new CompositeCell(column, row, region, firstRowCell, t.getGridTable());
        } else {
            Transform t1 = transform(0, firstRowCell.getHeight());// Properties parsing and merge
            if (t1 != null) {
                ICell propertiesCell = t1.grid().getCell(t1.getCol(), t1.getRow());
                if (row < firstRowCell.getHeight() + propertiesCell.getHeight() && PropertiesHelper.PROPERTIES_HEADER
                    .equals(propertiesCell.getStringValue())) {
                    Transform t2 = transform(propertiesCell.getWidth(), row);
                    if (t2 != null) {
                        ICell propertiesNameCell = t2.grid().getCell(t2.getCol(), t2.getRow());
                        Transform t3 = transform(propertiesCell.getWidth() + propertiesNameCell.getWidth(), row);
                        if (t3 != null) {
                            ICell propertiesValueCell = t3.grid().getCell(t3.getCol(), t3.getRow());
                            if (column >= propertiesCell.getWidth() + propertiesNameCell.getWidth()) {
                                IGridRegion reg = getRegionContaining(
                                    propertiesCell.getWidth() + propertiesNameCell.getWidth(),
                                    row);
                                IGridRegion region;
                                if (reg != null) {
                                    region = new GridRegion(reg.getTop(),
                                        reg.getLeft(),
                                        reg.getBottom(),
                                        reg.getLeft() + getWidth() - 1 - (propertiesCell.getWidth() + propertiesNameCell
                                            .getWidth()));
                                } else {
                                    region = new GridRegion(row,
                                        column,
                                        row,
                                        column + getWidth() - 1 - (propertiesCell.getWidth() + propertiesNameCell
                                            .getWidth()));
                                }
                                return new CompositeCell(column, row, region, propertiesValueCell, t3.getGridTable());
                            }
                        }
                    }
                }
            }
        }
        return super.getCell(column, row);
    }
}
