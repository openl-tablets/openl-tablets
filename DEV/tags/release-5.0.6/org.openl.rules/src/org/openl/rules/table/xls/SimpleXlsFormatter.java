/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.xls;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.table.IGrid;
import org.openl.rules.table.ui.FormattedCell;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.ui.IGridSelector;
import org.openl.rules.table.xls.XlsNumberFormat.SegmentFormatter;

/**
 * @author snshor
 *
 */
public class SimpleXlsFormatter implements IGridFilter
{

	public Object parse(String value)
	{
		throw new UnsupportedOperationException("This format does not parse");
	}

	static final public String GENERAL_XLS_FORMAT = "General";

	
	public IGridSelector getGridSelector()
	{
		return null;
	}

	public FormattedCell filterFormat(FormattedCell fc)
	{
		switch(fc.type)
		{
			case IGrid.CELL_TYPE_NUMERIC:
			case IGrid.CELL_TYPE_FORMULA:
				return formatNumberOrDate(fc);
		}
		
		return fc;
	}

	/**
	 * @param fc
	 * @return
	 */
	private FormattedCell formatNumberOrDate(FormattedCell fc)
	{
		// the only way to tell a date from a number (as far as I can understand)
		// is to check format, so let's do it
		
		if (isDateFormat(fc.getCellStyle().getTextFormat()))
			return formatDate(fc);
		
		return formatNumber(fc);
	}
	
	
	/**
	 * @param fc
	 * @return
	 */
	private FormattedCell formatDate(FormattedCell fc)
	{
		String fmt = fc.style.getTextFormat();
		XlsDateFormat xnf = findXlsDateFormat(fmt); 
		xnf.filterFormat(fc);
		
		
		return fc;
	}

	/**
	 * @param fmt
	 * @return
	 */
	private XlsDateFormat findXlsDateFormat(String fmt)
	{
		XlsDateFormat xnf = (XlsDateFormat)existingFormatters.get(fmt);
		if (xnf != null)
			return xnf;
		xnf = new XlsDateFormat(fmt);
		existingFormatters.put(fmt, xnf);
		return xnf;
	}

	static boolean isDateFormat(String fmt)
	{
		//fmt = fmt.toLowerCase();
		if (fmt==null)
			return false;
		
		if (GENERAL_XLS_FORMAT.equalsIgnoreCase(fmt))
			return false;
		
		if (containsAny(fmt, "#0?"))
			return false;

		return true;
	}
	
	
	static boolean isGeneralFormat(String fmt)
	{
		return fmt == null || GENERAL_XLS_FORMAT.equalsIgnoreCase(fmt);
	}
	

	
	static boolean containsAny(String src, String test)
	{
		char[] tst = test.toCharArray();
		for (int i = 0; i < tst.length; i++)
		{
			if (src.indexOf(tst[i]) >= 0)
				return true;
		}
		return false;
	}
	
	/**
	 * @param fc
	 * @return
	 */
	
	protected FormattedCell formatNumber(FormattedCell fc)
	{
		if (fc.style.getHorizontalAlignment() == ICellStyle.ALIGN_GENERAL)
			fc.style.setHorizontalAlignment(ICellStyle.ALIGN_RIGHT);
		
		String fmt = fc.style.getTextFormat();
		
		XlsNumberFormat xnf = findXlsNumberFormat(fmt); 
		xnf.filterFormat(fc);
		
		return fc;
	}

	/**
	 * @param fmt
	 * @return
	 */
	XlsNumberFormat findXlsNumberFormat(String fmt)
	{
		if (isGeneralFormat(fmt))
			return XlsNumberFormat.General;
		
		XlsNumberFormat xnf = (XlsNumberFormat)existingFormatters.get(fmt);
		if (xnf != null)
			return xnf;
		xnf = makeFormat(fmt);
		existingFormatters.put(fmt, xnf);
		return xnf;
	}

	Map<String, XlsFormat> existingFormatters = new HashMap<String, XlsFormat>();
	
	Map<String, SegmentFormatter> existingFmts = new HashMap<String, SegmentFormatter>();
	
	XlsNumberFormat makeFormat(String fmt)
	{
		if (GENERAL_XLS_FORMAT.equals(fmt))
			return XlsNumberFormat.General;
		
		return XlsNumberFormat.makeFormat(fmt, existingFmts); 
	}
	
}
