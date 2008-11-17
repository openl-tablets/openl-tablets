package org.openl.rules.helpers;

public interface ITableAdaptor
{
	int width(int row);
	int maxWidth();
	int height();
	
	Object get(int col, int row);
}
