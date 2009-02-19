package org.openl.rules.ui;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.ITreeElement;
import org.openl.util.TreeSorter;

public abstract class ATableTreeSorter extends TreeSorter
{
	
	
	public ITreeElement makeElement(Object obj, int i)
	{
		TableSyntaxNode tsn = (TableSyntaxNode)obj;
		Object so = makeSorterObject(tsn);
		
		ProjectTreeElement pte = new ProjectTreeElement(getDisplayValue(so, 0), getType(so), getUrl(so), getProblems(so), i, tsn);
		pte.setObject(so);
		return pte;
	}

	
	
	/**
	 * @param sorterObject TODO
	 * @return
	 */
	public Object getProblems(Object sorterObject)
	{
		return null;
	}



	public abstract Object makeSorterObject(TableSyntaxNode tsn);
	
	public abstract String[] getDisplayValue(Object sorterObject, int i);
	public abstract int getWeight(Object sorterObject);
	public abstract String getUrl(Object sorterObject);
	public abstract String getType(Object sorterObject);
	
	public abstract String getName();
	
	
	public Comparable makeKey(Object obj, int i)
	{
		TableSyntaxNode tsn = (TableSyntaxNode)obj;
		return makeTableKey(tsn, i);
	}
	
	public Comparable makeKey(Object obj)
	{
		return makeKey(obj, 0);
	}
	
	
	public Key makeTableKey(TableSyntaxNode tsn, int i)
	{
		
			
		Object so = makeSorterObject(tsn);
		return new Key(getWeight(so), 
				getDisplayValue(so, i));
	}

	static public class Key implements Comparable
	{
		public Key(int weight, String[] value)
		{
			this.weight = weight;
			this.value = value;
			
		}
		
		String[] value;
		int weight;
		
		public int compareTo(Object arg0)
		{
			Key key = (Key)arg0;
			if (weight == key.weight)
				return value[0].compareTo(key.value[0]);
			return weight - key.weight;
		}
	}
	
	static final public TableTypeSorter TABLE_TYPE_SORTER = new TableTypeSorter();

	static class TableTypeSorter extends ATableTreeSorter
	{

		public String[] getDisplayValue(Object sorterObject, int i)
		{
			TableSyntaxNode tsn = (TableSyntaxNode)sorterObject;
			String type = tsn.getType();
			Key wKey = (Key)getWeightMap().get(type);
			if (wKey == null)
				wKey = otherKey;
			return wKey.value;
		}
		
		static Key otherKey = new Key(10, new String[]{"Other","The Tables that do not belong to any known OpenL type",""}); 
		static Map weightMap;
		static Map getWeightMap()
		{
			if (weightMap == null)
			{
				weightMap = new HashMap();
				weightMap.put(ITableNodeTypes.XLS_DT, new Key(0, new String[]{"Decision","Decision Tables",""}));
				weightMap.put(ITableNodeTypes.XLS_SPREADSHEET, new Key(1, new String[]{"Spreadsheet","Spreadsheet Tables",""}));
				weightMap.put(ITableNodeTypes.XLS_TBASIC, new Key(2, new String[]{"TBasic","Structured Algorithm Tables",""}));
				weightMap.put(ITableNodeTypes.XLS_COLUMN_MATCH, new Key(3, new String[]{"Column Match","Column Match Tables",""}));

				weightMap.put(ITableNodeTypes.XLS_DATA, new Key(4, new String[]{"Data","Data Tables",""}));
				weightMap.put(ITableNodeTypes.XLS_TEST_METHOD, new Key(5, new String[]{"Test","Tables with data for method unit tests",""}));
				weightMap.put(ITableNodeTypes.XLS_RUN_METHOD, new Key(5, new String[]{"Run","Tables with run data for methods",""}));
				weightMap.put(ITableNodeTypes.XLS_DATATYPE, new Key(6, new String[]{"Datatype","OpenL Datatypes",""}));
				weightMap.put(ITableNodeTypes.XLS_METHOD, new Key(7, new String[]{"Method","OpenL Methods",""}));

				weightMap.put(ITableNodeTypes.XLS_ENVIRONMENT, new Key(8, new String[]{"Configuration","Environment table, used to configure OpenL project",""}));

			}	
			
			return weightMap;
		}
		

		public String getName()
		{
			return "Table Type";
		}

		public String getType(Object sorterObject)
		{
			TableSyntaxNode tsn = (TableSyntaxNode)sorterObject;
			
			
			return "folder."+tsn.getType();
		}

		public String getUrl(Object sorterObject)
		{
			return null;
		}

		public int getWeight(Object sorterObject)
		{
			TableSyntaxNode tsn = (TableSyntaxNode)sorterObject;
			String type = tsn.getType();
			Key wKey = (Key)getWeightMap().get(type);
			if (wKey == null)
				wKey = otherKey;
			return wKey.weight;
		}

		public Object makeSorterObject(TableSyntaxNode tsn)
		{
			return tsn;
		}


		/* (non-Javadoc)
		 * @see org.openl.rules.ui.ATableTreeSorter#getProblems()
		 */
		public Object getProblems(Object sorterObject)
		{
			// TODO Auto-generated method stub
			return null;
		}
		
		
	}
	
}
