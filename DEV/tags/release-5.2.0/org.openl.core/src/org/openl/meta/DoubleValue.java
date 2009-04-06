package org.openl.meta;


import java.text.DecimalFormat;
import java.text.Format;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.base.INamedThing;
import org.openl.util.AOpenIterator;
import org.openl.util.ITreeElement;

public class DoubleValue extends Number implements IMetaHolder, Comparable<Number>, ITreeElement<DoubleValue>, INamedThing
{
	private static final long serialVersionUID = -4594250562069599646L;

//	ValueMetaInfo metaInfo;
	IMetaInfo metaInfo;
	
	double value;
	
	String format = "#0.##";
	
	public DoubleValue(double value, IMetaInfo metaInfo,  String format) {
		super();
		this.metaInfo = metaInfo;
		this.value = value;
		this.format = format;
	}


	public int compareTo(Number o)
	{
		return Double.compare(value, ((Number)o).doubleValue());
	}


	public float floatValue()
	{
		return (float)value;
	}

	public int intValue()
	{
		return (int)value;
	}

	public long longValue()
	{
		return (long)value;
	}

	public DoubleValue(){}

	public DoubleValue(double value)
	{
		this.value = value;
	}

	public DoubleValue(double value, String name)
	{
		this.value = value;
		ValueMetaInfo mi = new ValueMetaInfo(); 
		mi.setShortName(name);
		metaInfo = mi;
	}
	
	public void setName(String name)
	{
		if (metaInfo == null)
			metaInfo = new ValueMetaInfo();
		if (metaInfo instanceof ValueMetaInfo)
			((ValueMetaInfo)metaInfo).setShortName(name);
	}

	public void setSourceUri(String uri)
	{
		if (metaInfo == null)
			metaInfo = new ValueMetaInfo();
		if (metaInfo instanceof ValueMetaInfo)
			((ValueMetaInfo)metaInfo).setSourceUrl(uri);
	}

	public void setFullName(String name)
	{
		if (metaInfo == null)
			metaInfo = new ValueMetaInfo();
		if (metaInfo instanceof ValueMetaInfo)
			((ValueMetaInfo)metaInfo).setFullName(name);
	}
	
	public String getName()
	{
		if (metaInfo == null)
			return null;
		return metaInfo.getDisplayName(IMetaInfo.LONG);
	}
	

	public DoubleValue(String valueString)
	{
		this.value = Double.parseDouble(valueString);
	}
	
	
	public IMetaInfo getMetaInfo()
	{
		return metaInfo;
	}

	public void setMetaInfo(IMetaInfo metaInfo)
	{
		this.metaInfo = metaInfo;
	}
	
	static public final int VALUE = 0x01, SHORT_NAME = 0x02, LONG_NAME = 0x04, 
	URL = 0x08, EXPAND_FORMULA = 0x10, EXPAND_FUNCTION = 0x20,
	PRINT_VALUE_IN_EXPANDED = 0x40,
	EXPAND_ALL = EXPAND_FORMULA | EXPAND_FUNCTION | PRINT_VALUE_IN_EXPANDED,
	PRINT_ALL = EXPAND_ALL | LONG_NAME;
	
	
	public static DoubleValue add(DoubleValue dv1, DoubleValue dv2)
	{
		if (dv1 == null  || dv1.getValue() == 0)
			return dv2;
		if (dv2 == null || dv2.getValue() == 0)
			return dv1;
		return new DoubleValueFormula(dv1, dv2, dv1.getValue() + dv2.getValue(), "+", false);
	}

	public static DoubleValue subtract(DoubleValue dv1,DoubleValue dv2)
	{
		if (dv2 == null || dv2.getValue() == 0)
			return dv1;
		return new DoubleValueFormula(dv1, dv2, dv1.getValue() - dv2.getValue(), "-", false);
	}

	
	static public DoubleValue subtract(double x, DoubleValue dv)
	{
		return new DoubleValueFormula(new DoubleValue(x), dv, x - dv.getValue(), "-", false);
	}

	static public DoubleValue subtract(int x, DoubleValue dv)
	{
		return new DoubleValueFormula(new DoubleValue(x), dv, x - dv.getValue(), "-", false);
	}
	
	
	
	public static DoubleValue multiply(DoubleValue dv1, DoubleValue dv2)
	{
		if (dv1.getValue() == 0 || dv2.getValue() == 0)
			return DoubleValue.ZERO;
		
		return new DoubleValueFormula(dv1, dv2, dv1.getValue() * dv2.getValue(), "*", true);
	}



	
	public static DoubleValue divide(DoubleValue dv1, DoubleValue dv2)
	{
		return new DoubleValueFormula(dv1, dv2, dv1.getValue() / dv2.getValue(), "/", true);
	}


	public static boolean gt(DoubleValue dv1, DoubleValue dv2)
	{
		return dv1.getValue() > dv2.getValue();
	}
	
	public static boolean ge(DoubleValue dv1, DoubleValue dv2)
	{
		return dv1.getValue() >= dv2.getValue();
	}

	public static boolean eq(DoubleValue dv1, DoubleValue dv2)
	{
		return dv1.getValue() == dv2.getValue();
	}

	public static boolean ne(DoubleValue dv1, DoubleValue dv2)
	{
		return dv1.getValue() != dv2.getValue();
	}

	
	public static boolean le(DoubleValue dv1, DoubleValue dv2)
	{
		return dv1.getValue() <= dv2.getValue();
	}
	
	
	public static boolean lt(DoubleValue dv1, DoubleValue dv2)
	{
		return dv1.getValue() < dv2.getValue();
	}
	
	public double getValue()
	{
		return value;
	}

	public void setValue(double value)
	{
		this.value = value;
	}
	

	static final public DoubleValue ZERO  = new DoubleValueZero();
	
	static class DoubleValueZero extends DoubleValue
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 3329865368482848868L;

		public double getValue(){return 0;}

		public DoubleValue add(DoubleValue dv)
		{
			return dv;
		}

		public DoubleValue divide(DoubleValue dv)
		{
			return this;
		}

		public DoubleValue multiply(DoubleValue dv)
		{
			return this;
		}

	}

	
	static final public DoubleValue ONE  = new DoubleValueOne();

	public static final DoubleValue[] EMPTY = {};


	
	static class DoubleValueOne extends DoubleValue
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 6347462002516785250L;



		public double getValue(){return 1;}



		public DoubleValue multiply(DoubleValue dv)
		{
			return dv;
		}

	}
	
	static public DoubleValue min(DoubleValue dv1, DoubleValue dv2)
	{
		return new DoubleValueFunction(dv2.getValue() < dv1.getValue()?
        dv2 : dv1, "min", new DoubleValue[]{dv1, dv2});
	}

	static public DoubleValue pow(DoubleValue dv1, DoubleValue dv2)
	{
		return new DoubleValueFunction(Math.pow(dv1.value, dv2.value), "pow", new DoubleValue[]{dv1, dv2});
	}
	
	
	static public DoubleValue max(DoubleValue dv1, DoubleValue dv2)
	{
		return new DoubleValueFunction(dv2.getValue() > dv1.getValue()?
        dv2 : dv1, "max", new DoubleValue[]{dv1, dv2});
	}

	
	static public DoubleValue round(DoubleValue dv1)
	{
		return new DoubleValueFunction(Math.round (dv1.getValue()), "round", new DoubleValue[]{dv1});
	}
	
	static public DoubleValue round(DoubleValue d, DoubleValue p)
	{
	
		if (d == null) 
			return ZERO; 
		return new DoubleValueFunction(Math.round(d.doubleValue()/p.doubleValue()) * p.doubleValue(), "round", new DoubleValue[]{d, p}) ;
	}
	
	
	public String printExplanation(int mode, boolean fromMultiplicativeExpr, List<String> urls)
	{
		
		if (urls != null && metaInfo != null && metaInfo.getSourceUrl() != null)
			urls.add(""+ metaInfo.getDisplayName(IMetaInfo.LONG) + " -> "+ metaInfo.getSourceUrl());
		return printExplanationLocal(mode, fromMultiplicativeExpr);
	}
	
	protected String printExplanationLocal(int mode, boolean fromMultiplicativeExpr)
	{
		switch(mode & (~EXPAND_ALL))
		{
			case VALUE:
				return printContent(mode, fromMultiplicativeExpr, false);
			case SHORT_NAME:
				return metaInfo == null ? printContent(mode, fromMultiplicativeExpr, false) : 
					metaInfo.getDisplayName(IMetaInfo.LONG) + "(" + printContent(mode, false, true) + ")";
			case LONG_NAME:
				return metaInfo == null ? printContent(mode, fromMultiplicativeExpr, false) : 
					metaInfo.getDisplayName(IMetaInfo.LONG) + "(" + printContent(mode, false, true) + ")";
			default:	
		}
		throw new RuntimeException("Wrong print mode!!");
		
	}
	
	protected String printContent(int mode, boolean fromMultiplicativeExpr, boolean inBrackets)
	{
		return printValue();
	}
	
		public String printValue()
		{
			return printValue(format);
		}
		
		
		public String printValue(String format)
		{
			return getFormat(format).format(value);
		}
		
		static Map<String, Format> formats = new HashMap<String, Format>();
		static synchronized Format getFormat(String fmt)
		{
			Format format = formats.get(fmt);
			if (format == null)
			{
				format = new DecimalFormat(fmt);
				formats.put(fmt, format);
			}
			return format;
		}
		

		public static DoubleValue negative(DoubleValue dv)
		{
			DoubleValue neg = new DoubleValue(-dv.value);
			neg.metaInfo = dv.metaInfo;
			return neg;
		}
	
		
		public static DoubleValue copy(DoubleValue value, String name)
		{
			if (value.getName() == null)
			{	
				value.setName(name);
				return value;
			}	
			else if (!value.getName().equals(name))
			{	
				DoubleValue dv = new DoubleValueFunction(value.doubleValue(), "COPY", new DoubleValue[]{value});
				dv.setName(name);
				return dv;
			}	
		
			return value;
		}
		
		
		public DoubleValue copy(String name)
		{
			return copy(this, name);
		}
		
		
//		public DoubleValue multiply(double d)
//		{
//			return multiply(new DoubleValue(d));
//		}
	
//		public DoubleValue divide(double d)
//		{
//			return divide(new DoubleValue(d));
//		}
	
		static public DoubleValue autocast(int x, DoubleValue y)
		{
			return new DoubleValue(x);
		}

		static public DoubleValue autocast(double x, DoubleValue y)
		{
			return new DoubleValue(x);
		}

		public String toString()
		{
			return printValue();
		}


		public double doubleValue()
		{
			return value;
		}


		public String getFormat()
		{
			return this.format;
		}


		public void setFormat(String format)
		{
			this.format = format;
		}


		public String getType()
		{
			return "value";
		}


		public DoubleValue getObject()
		{
			return this;
		}


		public Iterator<DoubleValue> getChildren()
		{
			return AOpenIterator.empty();
		}


		public boolean isLeaf()
		{
			return true;
		}


		public String getDisplayName(int mode)
		{
			switch(mode)
			{
			  case SHORT:
			  	return printValue();
			  default:
			  	String name = metaInfo == null ? null : getMetaInfo().getDisplayName(mode);
			  	return name == null ?  printValue() : name + "(" + printValue() + ")";
			}
		}
		
		
		
		
		
}
