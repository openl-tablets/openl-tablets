package org.openl.rules.ui;

import org.openl.rules.indexer.IIndexElement;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class WorksheetSorter extends ATableTreeSorter implements IProjectTypes
{

	public String[] getDisplayValue(Object sorterObject, int i)
	{
		XlsSheetSourceCodeModule sheet = (XlsSheetSourceCodeModule)sorterObject;
		return new String[]{sheet.getSheetName(),sheet.getSheetName(), sheet.getSheetName()};
	}

	public String getName()
	{
		return "worksheet";
	}

	public String getType(Object sorterObject)
	{
		return PT_WORKSHEET;
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

	public Object makeSorterObject(TableSyntaxNode tsn)
	{
		return tsn.getModule();
	}
	
}
