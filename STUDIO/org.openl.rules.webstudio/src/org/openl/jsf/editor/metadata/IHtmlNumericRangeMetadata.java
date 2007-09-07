package org.openl.jsf.editor.metadata;

public interface IHtmlNumericRangeMetadata {

	public double getMin();
	public double getMax();
	
	public boolean isMinClosed();
	public boolean isMaxClosed();
}
