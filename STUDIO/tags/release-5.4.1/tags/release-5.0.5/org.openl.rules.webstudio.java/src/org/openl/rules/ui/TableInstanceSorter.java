package org.openl.rules.ui;

import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.StringTool;

public class TableInstanceSorter extends ATableTreeSorter implements IProjectTypes, ITableNodeTypes
{

	protected boolean isUnique()
	{
		return true;
	}




	public String[] getDisplayValue(Object sorterObject, int i)
	{
		TableSyntaxNode tsn  = (TableSyntaxNode)sorterObject;
		return getTableDisplayValue(tsn, i);
	}
	
	static public String[] getTableDisplayValue(TableSyntaxNode tsn)
	{
		return getTableDisplayValue(tsn, 0);
	}
		
		static public String[] getTableDisplayValue(TableSyntaxNode tsn, int i)
		{
		TableProperties tp = tsn.getTableProperties();
		String display = null;
		String name = null;
		
		
		if (tp != null)
		{	
			
			name = tp.getPropertyValue("name");
			display = tp.getPropertyValue("display");
			if (display == null)
				display = name;
		}
		
		
		if (name == null)
		  name =  str2name(tsn.getTable().getGridTable().getStringValue(0, 0), tsn.getType());
		
		if (display == null)
		  display =  str2display(tsn.getTable().getGridTable().getStringValue(0, 0), tsn.getType());

		String sfx =(i < 2 ? "":"("+i+")");
		return new String[]{name + sfx, display + sfx, display + sfx};
	}
	
	
	static String str2name(String src, String type)
	{
		if (src == null)
			src = "NO NAME";
		
		String[] tokens = StringTool.tokenize(src, " \n\r(),");
		
		if (XLS_DT.equals(type) && tokens.length >= 3)
		{
			return tokens[2];
		}
		
		if (XLS_DATA.equals(type) && tokens.length >= 3)
		{
			return tokens[2];
		}
		
		if (XLS_TEST_METHOD.equals(type) && tokens.length >= 3)
			return tokens[2];

		if (XLS_RUN_METHOD.equals(type) && tokens.length >= 3)
			return tokens[2];
	
		if (XLS_METHOD.equals(type) && tokens.length >= 3)
			return tokens[2];
		
		return src;
	}
	
	static String str2display(String src, String type)
	{
//		String[] tokens = StringTool.tokenize(src, " \n\r(),");
//		
//		if (XLS_DT.equals(type) && tokens.length >= 3)
//		{
//			return tokens[2];
//		}
//		
//		if (XLS_DATA.equals(type) && tokens.length >= 3)
//		{
//			return tokens[2];
//		}
//		
//		if (XLS_TEST_METHOD.equals(type) && tokens.length >= 3)
//			return tokens[2];
//			
//		if (XLS_METHOD.equals(type) && tokens.length >= 3)
//			return tokens[2];
		
		return src;
	}
	

	public String getName()
	{
		return "Table Instance";
	}

	public String getType(Object sorterObject)
	{
		TableSyntaxNode tsn  = (TableSyntaxNode)sorterObject;
		return PT_TABLE + "." + tsn.getType();
	}

	public String getUrl(Object sorterObject)
	{
		TableSyntaxNode tsn  = (TableSyntaxNode)sorterObject;
		return tsn.getUri();
	}

	public int getWeight(Object sorterObject)
	{
		return 0;
	}

	public Object makeSorterObject(TableSyntaxNode tsn)
	{
		return tsn;
	}


	/* (non-Javadoc)
	 * @see org.openl.rules.ui.ATableTreeSorter#getProblems(java.lang.Object)
	 */
	public Object getProblems(Object sorterObject)
	{
		TableSyntaxNode tsn  = (TableSyntaxNode)sorterObject;
		
		return tsn.getErrors();
	}

}
