package org.openl.rules.table.ui.filters;

import org.openl.rules.table.ui.IGridSelector;

public abstract class AGridFilter implements IGridFilter {

    private IGridSelector selector;

    public AGridFilter() {
    }

    public AGridFilter(IGridSelector selector) {
        this.selector = selector;
    }

    public IGridSelector getGridSelector() {
        return selector;
    }

}