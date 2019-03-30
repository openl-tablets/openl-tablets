package org.openl.excel.grid;

import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;

public class EditableGridTable extends GridTable {
    private final ParsedGrid grid;

    public EditableGridTable(IGridTable delegate) {
        super(delegate.getRegion(), delegate.getGrid());
        this.grid = (ParsedGrid) delegate.getGrid();
    }

    @Override
    public IGrid getGrid() {
        return grid.isEditing() ? grid.getWritableGrid() : super.getGrid();
    }

    @Override
    public void edit() {
        grid.getWritableGrid();
    }

    @Override
    public void stopEditing() {
        grid.stopEditing();
    }
}
