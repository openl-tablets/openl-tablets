package org.openl.rules.indexer;

public interface IIndexParser
{
	IIndexElement[] parse(IIndexElement root);
	
	String getType();
	String getCategory();
}
