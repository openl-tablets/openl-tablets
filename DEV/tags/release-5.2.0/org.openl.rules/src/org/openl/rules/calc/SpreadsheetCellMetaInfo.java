package org.openl.rules.calc;

import org.openl.IOpenSourceCodeModule;
import org.openl.meta.IMetaInfo;

public class SpreadsheetCellMetaInfo implements IMetaInfo 
{

	String name;
	IOpenSourceCodeModule src;
	
	public SpreadsheetCellMetaInfo(String name, IOpenSourceCodeModule src) {
		super();
		this.name = name;
		this.src = src;
	}

	public String getDisplayName(int mode) {
		return name;
	}

	public String getSourceUrl() {
		return src.getUri(0);
	}

}
