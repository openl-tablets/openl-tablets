package org.openl.jsf.editor.metadata;

import java.util.Date;

public interface IHtmlDateRangeMetadata {
	//
	public Date getMin();
	public Date getMax();
	
	public boolean isMinClosed();
	public boolean isMaxClosed();
}
