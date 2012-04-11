package org.openl.rules.ui.jsf.custom.tableeditor.model;


/**
 * NOT FULLY DOCUMENTED.
 *
 * The table editor component implementation might be changed significantly.
 *
 * @author Aliaksandr Antonik
 */
public interface CellModel {
	public int getColspan();
	public int getRowspan();
	public boolean isReal();
}
