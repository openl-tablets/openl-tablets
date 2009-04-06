package org.openl.rules.indexer;

public interface IIndexer
{
	String getType();
	String getCategory();
	
	void index(IIndexElement element, Index index);

}
