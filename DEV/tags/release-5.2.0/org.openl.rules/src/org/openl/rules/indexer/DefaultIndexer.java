package org.openl.rules.indexer;

public class DefaultIndexer implements IIndexer
{

	public String getCategory()
	{
		return "All";
	}

	public String getType()
	{
		return "All";
	}

	public void index(IIndexElement element, Index index)
	{
		String src = element.getIndexedText();
		if (src == null)
			return;
		Tokenizer tt = new Tokenizer(src);
		
		String[] tokens = tt.parse();
		
		for (int i = 0; i < tokens.length; i++)
		{
			index.add(tokens[i], element);
		}
	}

	
	
}
