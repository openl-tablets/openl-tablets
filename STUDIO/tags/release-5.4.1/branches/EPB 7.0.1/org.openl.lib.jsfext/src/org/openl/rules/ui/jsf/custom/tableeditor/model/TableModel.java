package org.openl.rules.ui.jsf.custom.tableeditor.model;

/**
 * NOT FULLY DOCUMENTED.
 *
 * The table editor component implementation might be changed significantly.
 *
 * @author Aliaksandr Antonik
 */
public abstract class TableModel {
	abstract public CellModel[][] getCells();

	public boolean isCellAvailable(int row, int col) {
		return row >= 0 && col >= 0 && row < getCells().length && col < getCells()[row].length;
	}
}
