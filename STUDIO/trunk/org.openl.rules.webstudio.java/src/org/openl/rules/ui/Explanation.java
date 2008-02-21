package org.openl.rules.ui;

import java.util.Iterator;
import java.util.Vector;

import org.openl.meta.DoubleValue;
import org.openl.meta.DoubleValueFormula;
import org.openl.meta.DoubleValueFunction;
import org.openl.meta.IMetaInfo;
import org.openl.meta.ValueMetaInfo;
import org.openl.rules.webtools.WebTool;
import org.openl.rules.webtools.XlsUrlParser;
import org.openl.util.OpenIterator;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;
import org.openl.util.TreeIterator;

public class Explanation
{
	DoubleValue root;

	Vector expandedValues = new Vector();
	
//	String pname;
//	String period;
	
	String header;
	
	boolean showNamesInFormula = false;
	boolean showValuesInFormula=true;
	
	Explanator explanator;
	
	public Explanation(Explanator explanator)
	{
		this.explanator = explanator;
	}
	
	public String[] htmlTable(DoubleValue value)
	{
		String text = value.printValue();
		String url = findUrl(value, null);
		ValueMetaInfo mi = (ValueMetaInfo) value.getMetaInfo();
		String name = mi != null ?  mi.getDisplayName(IMetaInfo.LONG) : null ;
		
		if (url != null)
			text = WebTool.urlLink(makeUrl(url), "show", text, null);
		
		if (name == null)
			name = "";
		else if (url != null)
			name = WebTool.urlLink("showExplainTable.jsp?uri=" + StringTool.encodeURL(url) + "&text=" + name, "show", name, "mainFrame");
		
		return new String[]{text, name, htmlString(value)};
	}

	public String htmlString(DoubleValue value)
	{
		if (value.getClass() == DoubleValueFormula.class)
		{
			return expandFormula((DoubleValueFormula) value, null, 0);
		} else if (value.getClass() == DoubleValueFunction.class)
		{
			return expandFunction((DoubleValueFunction) value, null);
		}
		return expandValue(value);
	}
	
	
	

	private String expandFunction(DoubleValueFunction function, String parentUrl)
	{
		String url = findUrl(function, parentUrl);
		String ret = function.getFunctionName() + "(";
		DoubleValue[] params = function.getParams();
		
		for (int i = 0; i < params.length; i++)
		{
			if (i > 0)
				ret += ", ";
			ret += expandArgument(params[i], false, url, 0);
		}
		return ret + ")";
	}




	protected String expandFormula(DoubleValueFormula formula, String parentUrl, int level)
	{
		
		
		String url = findUrl(formula, parentUrl);
		return expandArgument(formula.getDv1(), formula.isMultiplicative(), url, level)
				+ formula.getOperand()
				+ expandArgument(formula.getDv2(), formula.isMultiplicative(), url, level);
	}

	static final int MAX_LEVEL = 2; //Maximum expansion level for formulas 
	
	protected String expandArgument(DoubleValue value, boolean isMultiplicative,
			String parentUrl, int level)
	{
		String url = findUrl(value, parentUrl);
		if (value.getClass() == DoubleValueFormula.class)
		{
			DoubleValueFormula formula = (DoubleValueFormula) value;
			if (formula.isMultiplicative() == isMultiplicative && level < MAX_LEVEL)
				return expandFormula(formula, url, level + 1);
		}

		return expandValue(value);

	}

	
	static String getName(DoubleValue value )
	{
		ValueMetaInfo mi = (ValueMetaInfo) value.getMetaInfo();
		String name = mi != null ?  mi.getDisplayName(IMetaInfo.LONG) : null ;
		return name;
	}
	
	public String expandValue(DoubleValue value)
	{
		String text = value.printValue();
		
//		String url = findUrl(value, parentUrl);

		String name = getName(value);
		
		if (name != null && showNamesInFormula)
		{
			if (showValuesInFormula)
				text = name + "(" + text + ")";
			else
				text = name;
		}	
		
		if (this.expandedValues.contains(value))
			return text;
	 int id = explanator.getUniqueId(value);
	 
   return WebTool.urlLink(makeExpandUrl(id), name == null ? "expand" : name, text, null);

		
		//		if (isExpandable(value))
//		{
//			int id = getUniqueId(value);
//			return urlLink(makeExpandUrl(id), "expand", text);
//		} else
//		{
//			return urlLink(makeUrl(url), name, text);
//		}

	}


	protected String makeExpandUrl(int id)
	{
		return makeBasicUrl() 
		+ "&expandID=" + id 
		;
	}
	
	protected String makeBasicUrl()
	{
//		return "explain.jsp?rootID=" + explanator.getUniqueId(root) 
		return "?rootID=" + explanator.getUniqueId(root) 
		+ "&header=" + header 
//		+ "&pname=" + pname 
//		+ "&period=" + period  
		+ (showNamesInFormula ? "&showNames=true" : "") 
		+ (showValuesInFormula ? "&showValues=true" : "") 
		;
	}
	
	

	protected String makeUrl(String url)
	{
		if (url == null)
			return "#";
		
		XlsUrlParser parser = new XlsUrlParser();
		try
		{
			parser.parse(url);
		} catch (Exception e)
		{
			throw RuntimeExceptionWrapper.wrap(e);
		}
		
		String ret = makeBasicUrl()		
		+ "&wbPath=" + parser.wbPath
		+ "&wbName=" + parser.wbName
		+ "&wsName=" + parser.wsName
		+ "&range=" + parser.range
		;
				
		return ret;
	}

	protected boolean isExpandable(DoubleValue value)
	{
		return value.getClass() == DoubleValueFormula.class
				|| value.getClass() == DoubleValueFunction.class;
	}


	public String findUrl(DoubleValue value, String parentUrl)
	{
		ValueMetaInfo mi = (ValueMetaInfo) value.getMetaInfo(); 
		
		String url = mi != null ? mi.getSourceUrl() : null;
		if (url == null)
			return parentUrl;
		return url;

	}

	static class DoubleValueIterator implements TreeIterator.TreeAdaptor
	{

		public Iterator children(Object node)
		{
			if (node.getClass() == DoubleValueFormula.class)
			{
				return OpenIterator.fromArray(((DoubleValueFormula) node)
						.getArguments());
			} else if (node.getClass() == DoubleValueFunction.class)
			{
				return OpenIterator.fromArray(((DoubleValueFunction) node).getParams());
			} else
				return OpenIterator.EMPTY;
		}

	}
	


	public Vector getExpandedValues()
	{
		return expandedValues;
	}

	public void expand(String expandID)
	{
		DoubleValue dv = explanator.find(expandID); 
		if (!expandedValues.contains(dv))
			expandedValues.add(dv);
	}


	public void setExpandedValues(Vector expandedValues)
	{
		this.expandedValues = expandedValues;
	}




	public DoubleValue getRoot()
	{
		return root;
	}




	public void setRoot(DoubleValue root)
	{
		this.root = root;
	}

//	public String getPeriod()
//	{
//		return period;
//	}
//
//	public void setPeriod(String period)
//	{
//		this.period = period;
//	}
//
//	public String getPname()
//	{
//		return pname;
//	}
//
//	public void setPname(String pname)
//	{
//		this.pname = pname;
//	}

	
	
	
	public boolean isShowNamesInFormula()
	{
		return showNamesInFormula;
	}

	public void setShowNamesInFormula(boolean showNamesInFormula)
	{
		this.showNamesInFormula = showNamesInFormula;
	}

	public String getHeader()
	{
		return header;
	}

	public void setHeader(String header)
	{
		this.header = header;
	}

	public boolean isShowValuesInFormula()
	{
		return this.showValuesInFormula;
	}

	public void setShowValuesInFormula(boolean showValuesInFormula)
	{
		this.showValuesInFormula = showValuesInFormula;
	}
}
