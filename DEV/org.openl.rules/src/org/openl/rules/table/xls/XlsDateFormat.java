/**
 * Created Feb 28, 2007
 */
package org.openl.rules.table.xls;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openl.rules.table.ui.FormattedCell;
import org.openl.util.Log;

/**
 * @author snshor
 *
 */
public class XlsDateFormat extends XlsFormat
{
	
	SimpleDateFormat format;
	
	public XlsDateFormat(String fmt)
	{
		String javaFormat = convertTojavaFormat(fmt);
		format = new SimpleDateFormat(javaFormat);
	}

	/**
	 * 
	 * @param fmt
	 * @return
	 */
	static public String convertTojavaFormat(String fmt)
	{
//TODO this will require much more work than that
		
		return fmt.replace('m', 'M');
	}

	public XlsDateFormat(SimpleDateFormat fmt)
	{
		format = fmt;
	}

	public FormattedCell filterFormat(FormattedCell cell)
	{
		if (cell.value == null)
			return cell;

		if (!(cell.value instanceof Date))
		{
			Log.error("Should be date" + cell.value);
			return cell;
		}	
		
		Date date = (Date)cell.value;
		
		cell.content = format.format(date);
		cell.setFilter(this);
		
		return cell;
	}

	public Object parse(String value)
	{
		try
		{
			return format.parse(value);
		} catch (ParseException e)
		{
			Log.warn("Could not parse Date: " + value, e);
			return value;
		}
	}



}
