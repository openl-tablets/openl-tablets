/**
 * Created Feb 26, 2007
 */
package org.openl.rules.table.xls;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.table.ui.FormattedCell;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.ITextFormatter;
import org.openl.util.Log;
import org.openl.util.StringTool;

/**
 * This class provides default conversion of MS Excel formats to Java formats.
 * There is no way for practical and technical reasons to map it completely
 * 100%. Therefore this class will be supplemented by a) pre-defined hardcoded
 * mapping for most of embedded MS Excel formats; b) by providing app developers
 * with ability to plug-in their own custom transformers and/or mappings.
 * 
 * @author snshor
 * 
 */
public class XlsNumberFormat extends  XlsFormat
{

	static final public DecimalFormat DEFAULT_FORMAT = new DecimalFormat("0.00");
	static final public String GENERAL_FORMAT_STR = "#.######";
	static final public DecimalFormat GENERAL_FORMAT = new DecimalFormat(GENERAL_FORMAT_STR);

	
	static final public XlsNumberFormat General = new XlsNumberFormat(
			new SegmentFormatter(new ITextFormatter.NumberTextFormatter(GENERAL_FORMAT, GENERAL_FORMAT_STR), null) ,null, null);
	
	
	
	SegmentFormatter positiveFormat;

	SegmentFormatter negativeFormat;

	SegmentFormatter zeroFormat;

	public XlsNumberFormat(SegmentFormatter positiveFormat,
			SegmentFormatter negativeFormat, SegmentFormatter zeroFormat)
	{
		this.positiveFormat = positiveFormat;
		this.negativeFormat = negativeFormat;
		this.zeroFormat = zeroFormat;
	}
	
	
	
	
	public Object parse(String value)
	{
		return positiveFormat.parse(value);
	}




	static public XlsNumberFormat makeFormat(String xlsformat, Map<String, SegmentFormatter> existingFmts)
	{
		String[] fmts = StringTool.tokenize(xlsformat, ";");
		
		
		int N = 3;
		int NEG =1;
		SegmentFormatter[] sff = new SegmentFormatter[N];
		int len = Math.min(fmts.length, N);
		
		for(int i = 0; i < len ; ++i)
		{
			SegmentFormatter sf = getFormat(fmts[i], existingFmts, i == NEG);
			sff[i] = sf;
		}	
		
		return new XlsNumberFormat(sff[0], sff[1], sff[2]);
		
	}
	
	

	/**
	 * @param string
	 * @param existingFmts
	 * @return
	 */
	private static SegmentFormatter getFormat(String fmt, Map<String, SegmentFormatter> existingFmts, boolean isNegative)
	{
		SegmentFormatter sf = existingFmts.get(fmt);
		if (sf != null) return sf;
		
		sf = makeSegmentFormatter(fmt, isNegative);
		existingFmts.put(fmt, sf);
		return sf;
	}

	

	/**
	 * @param fmt
	 * @return
	 */
	static SegmentFormatter makeSegmentFormatter(String fmt, boolean isNegative)
	{
		
		SegmentFormatter sf = new SegmentFormatter();
		
		fmt  = detectColor(sf, fmt);
		
		String javaFormat =  transformToJavaFormat(fmt, sf);
		if (isNegative)
		{
			javaFormat = StringTool.removeChars(javaFormat,"()") + ';' + javaFormat;
		}	
		
		if (javaFormat.indexOf('#') < 0 && javaFormat.indexOf('0') < 0)
		{
			ITextFormatter tf = new ITextFormatter.ConstTextFormatter(javaFormat);
			sf.format = tf;
			return sf;
			
		}	
		
		DecimalFormat df = null;
		try
		{
			df = new DecimalFormat(javaFormat);
		} 
		catch (Throwable t)
		{
			Log.warn("Bad java format. Using default. Consider custom mapping: '" + javaFormat + "'");
			df = DEFAULT_FORMAT;
		}
		
		ITextFormatter tf = new ITextFormatter.NumberTextFormatter(df, javaFormat);
		sf.format = tf;
		
		return sf;
	}




	/**
	 * @param sf
	 * @param fmt
	 * @return
	 */
	private static String detectColor(SegmentFormatter sf, String fmt)
	{
		for (int i = 0; i < colorsStr.length; i++)
		{
			if (fmt.startsWith(colorsStr[i]))
			{
				sf.color = colors[i];
				return fmt.substring(colorsStr[i].length());
			}	
		}
		
		return fmt;
	}



	
	public String format(Number value)
	{
		SegmentFormatter sf = positiveFormat;

		if (value.doubleValue() < 0)
			sf = negativeFormat();

		else if (value.doubleValue() == 0)
			sf = zeroFormat();

		if (sf.multiplier != 1)
		{
			value = new Double(value.doubleValue() * sf.multiplier);
		}	
		
		return sf.format.format(value);
		
	}

	public FormattedCell filterFormat(FormattedCell fcell)
	{
		if (fcell.value == null)
			return fcell;

		if (fcell.value instanceof String)
		{
			Log.error("Should be Number " + fcell.value);
			return fcell;
		}	
		
		Number value = (Number) fcell.value;
		
		SegmentFormatter sf = positiveFormat;

		
		if (value.doubleValue() < 0)
			sf = negativeFormat();

		else if (value.doubleValue() == 0)
			sf = zeroFormat();

		if (sf.multiplier != 1)
		{
			value = new Double(value.doubleValue() * sf.multiplier);
		}	
		
		fcell.content = sf.format.format(value);
		
		if (fcell.font.getFontColor() == null)
			fcell.font.setFontColor(sf.color);
		
		if (fcell.style.getHorizontalAlignment() == ICellStyle.ALIGN_GENERAL)
			fcell.style.setHorizontalAlignment(sf.alignment);
		
		fcell.setFilter(this);
		return fcell;
	}

	/**
	 * @return
	 */
	private SegmentFormatter zeroFormat()
	{
		return zeroFormat == null ? positiveFormat : zeroFormat;
	}

	/**
	 * @return
	 */
	private SegmentFormatter negativeFormat()
	{
		return negativeFormat == null ? positiveFormat : negativeFormat;
	}

	static public class SegmentFormatter
	{
		ITextFormatter format;

		short[] color;
		
		double multiplier = 1;

		int alignment = ICellStyle.ALIGN_GENERAL;

		public SegmentFormatter(ITextFormatter format, short[] color, int alignment)
		{
			this.format = format;
			this.color = color;
			this.alignment = alignment;
		}

		public SegmentFormatter(ITextFormatter format, short[] color)
		{
			this.format = format;
			this.color = color;
		}

		public Object parse(String value)
		{
			return format.parse(value);
		}
		
		
		/**
		 * 
		 */
		public SegmentFormatter()
		{
			// TODO Auto-generated constructor stub
		}
	}

	public static void main(String[] args)
	{
		
		System.out.println(new DecimalFormat("$#,##0").format(12.334)+"|"); 
		
		
		System.out.println(new DecimalFormat(" #,##0.0; (#,##0.0)").format(-12.334)+"|"); 
		
		double[] x = { -12.35, 12345.6789, 8.9, .631, 12, 12.35, };

		String[] formats = {"$#,##0_);[Red]($#,##0)","_(* #,##0.0_);_(* (#,##0.0);_(* \"-\"??_);_(@_)" , "#,###", "#,", "####.#", "#.000", "0.#", "#.0#",
				"???.???", "#.0;(#.0)", };

		HashMap<String, SegmentFormatter> existingFmts = new HashMap<String, SegmentFormatter>();
		for (int i = 0; i < formats.length; i++)
		{
			XlsNumberFormat  xnf =  makeFormat(formats[i], existingFmts);
//			NumberFormat f = new DecimalFormat(jf);
			System.out.println(formats[i] + "  :  ");
			for (int j = 0; j < x.length; j++)
			{
				Double value = new Double(x[j]);
				
				
				System.out.println(xnf.format(value) + "|  " + x[j]);
			}
			System.out.println();
		}
//		for (int i = 0; i < formats.length; i++)
//		{
//			SegmentFormatter sf = new SegmentFormatter();
//			String jf = transformToJavaFormat(formats[i], sf);
//			NumberFormat f = new DecimalFormat(jf);
//			System.out.println(formats[i] + "  :  " + jf);
//			for (int j = 0; j < x.length; j++)
//			{
//				System.out.println(f.format(x[j] * sf.multiplier) + "|");
//			}
//			System.out.println();
//		}
	}

	static final String[] colorsStr = { "[Black]", "[Blue]", "[Cyan]", "[Green]",
			"[Magenta]", "[Red]", "[White]", "[Yellow]" };

	static final short[][] colors = { { 0x00, 0x00, 0x00 }, { 0x00, 0x00, 0xff },
			{ 0x00, 0xff, 0xff }, { 0x00, 0xff, 0x00 }, { 0xff, 0x00, 0xff },
			{ 0xff, 0x00, 0x00 }, { 0xff, 0xff, 0xff }, { 0xff, 0xff, 0x00 }, };

	static class TransformedFormat
	{
		double multiplier = 1;

		String javaFormat;

		short[] color;
	}

	static public String transformToJavaFormat(String xlsFormat,
			SegmentFormatter sf)
	{

		
		StringBuffer res = new StringBuffer();
		
		boolean skip = false;
		
		for (int i = 0; i < xlsFormat.length(); i++)
		{
			if (skip)
			{
				skip = false;
				continue;
			}	
			char c = xlsFormat.charAt(i);
			
			switch(c)
			{
				case '_':
					res.append(' ');
					skip = true;
					continue;
				case 	'*':
					skip = true;
					continue;
				case '\\':
				case '"':
					continue;
				default:
					res.append(c);
			}
		}
		
		
		xlsFormat = res.toString();
		
		
		// transform trailing commas

		while (xlsFormat.endsWith(","))
		{
			sf.multiplier /= 1000;
			xlsFormat = xlsFormat.substring(0, xlsFormat.length() - 1);
		}

		// TODO sure it works differently, but do we want to deal with it? Not for
		// now.
		if (xlsFormat.indexOf(".?") >= 0)
			xlsFormat = xlsFormat.replace('?', '#');
		else
			xlsFormat = xlsFormat.replace('?', ' ');

		return xlsFormat;
	}


}
