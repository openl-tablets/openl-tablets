/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import org.openl.rules.table.IGridTable;

/**
 * @author snshor
 *
 */
public interface ICellSelector {
    public boolean select(IGridTable table, int col, int row);
}
