package org.openl.jsf.editor.metadata.impl;

import java.util.List;

import org.openl.jsf.editor.metadata.IHtmlSelectMetadata;

public class HtmlSelectMetadata implements IHtmlSelectMetadata {

	protected List list;
	
	@Override
	public List getList() {
		return list;
	}

	public void setList(List list) {
		this.list = list;
	}
	
}
