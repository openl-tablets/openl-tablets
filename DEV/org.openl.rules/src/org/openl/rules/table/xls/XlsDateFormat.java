/**
 * Created Feb 28, 2007
 */
package org.openl.rules.table.xls;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openl.rules.table.ui.AGridFilter;
import org.openl.rules.table.ui.FormattedCell;
import org.openl.util.Log;

/**
 * @author snshor
 *
 */
public class XlsDateFormat extends AGridFilter
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
		
		return cell;
	}



}
