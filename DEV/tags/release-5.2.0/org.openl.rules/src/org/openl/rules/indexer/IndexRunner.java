package org.openl.rules.indexer;

public class IndexRunner
{
	
	public IndexRunner(IIndexParser[] parsers, IIndexer[] indexers, IIndexer defaultIndexer)
	{
		this.parsers = parsers;
		this.indexers = indexers;
		this.defaultIndexer = defaultIndexer;
	}
	
	
	public void index(IIndexElement element, Index index)
	{
		IIndexParser parser = findParser(element);
		IIndexer indexer = findIndexer(element);
		
		if (indexer != null)
		{
			indexer.index(element, index);
		}
		
		if (parser != null)
		{
			IIndexElement[] elements = parser.parse(element);
			for (int i = 0; i < elements.length; i++)
			{
				index(elements[i], index);
			}
		}	
	}

	IIndexer[] indexers;
	protected IIndexer defaultIndexer = new DefaultIndexer();
	
	private IIndexer findIndexer(IIndexElement element)
	{
		for (int i = 0; i < indexers.length; i++)
		{
			if (indexers[i].getType().equals(element.getType()) && indexers[i].getCategory().equals(element.getCategory()))
			  return indexers[i];		
		}
		
		for (int i = 0; i < indexers.length; i++)
		{
			if (indexers[i].getCategory().equals(element.getCategory()))
			  return indexers[i];		
		}
		
		return defaultIndexer;
	}

	private IIndexParser findParser(IIndexElement element)
	{
		for (int i = 0; i < parsers.length; i++)
		{
			if (parsers[i].getType().equals(element.getType()) && parsers[i].getCategory().equals(element.getCategory()))
			  return parsers[i];		
		}
		
		for (int i = 0; i < parsers.length; i++)
		{
			if (parsers[i].getCategory().equals(element.getCategory()))
			  return parsers[i];		
		}

		return null;
	}

	IIndexParser[] parsers;
	


}
