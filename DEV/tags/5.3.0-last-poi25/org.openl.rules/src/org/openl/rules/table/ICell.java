package org.openl.rules.table;

import org.openl.rules.table.ui.ICellStyle;

public interface ICell {
	
	ICellStyle getCellStyle();
	
	int getCellWidth();
	
	Object getObjectValue();
	
	String getStringValue();
	
	ICellInfo getCellInfo();
	
	int getCellHeight();
}
