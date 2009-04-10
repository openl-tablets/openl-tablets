package org.openl.rules.ui;

import org.openl.rules.indexer.IIndexElement;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class CategorySorter extends ATableTreeSorter implements IProjectTypes
{

	public String[] getDisplayValue(Object sorterObject, int i)
	{
		String category = getCategory((TableSyntaxNode)sorterObject);
		return new String[]{category, category, category};
	}

	public String getName()
	{
		return "category";
	}

	public String getType(Object sorterObject)
	{
		return "category";
	}

	public String getUrl(Object sorterObject)
	{
		IIndexElement ie = (IIndexElement)sorterObject;
		return ie.getUri();
	}

	public int getWeight(Object sorterObject)
	{
		return 0;
	}

	String getCategory(TableSyntaxNode tsn)
	{
		String category = tsn.getProperty("category");
		
		if (category == null)
		{	
		
			XlsSheetSourceCodeModule sheet = tsn.getXlsSheetSourceCodeModule();
			category = sheet.getSheetName();
		}
		return category;
		
	}
	
	
	public Object makeSorterObject(TableSyntaxNode tsn)
	{
		return tsn;
	}
	
}
