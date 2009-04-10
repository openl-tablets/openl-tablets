package org.openl.rules.table.ui;

public interface ICellFont
{

	short[] getFontColor();
	
	int getSize();
	
	String getName();
	
	boolean isItalic();
	boolean isBold();
	boolean isUnderlined();
	boolean isStrikeout();
	
	
}
