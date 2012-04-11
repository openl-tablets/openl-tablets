/*
 * Created on Jul 1, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules.table.xls;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.openl.rules.table.ui.ICellStyle;


/**
 * @author snshor
 */
public class XlsCellStyle implements ICellStyle
{
	HSSFCellStyle xlsStyle;
	HSSFWorkbook workbook;
	
	public XlsCellStyle(HSSFCellStyle xlsStyle, HSSFWorkbook workbook)
	{
		this.xlsStyle = xlsStyle;
		this.workbook = workbook;
	}
	
	

	public short[] getFillBackgroundColor()
	{
		short x = xlsStyle.getFillBackgroundColor();
		return colorToArray(x, workbook);
	}
	
	
	
	




	public int getIdent()
	{
		return xlsStyle.getIndention();
	}

	


	public int getRotation()
	{
		return xlsStyle.getRotation();
	}



	public boolean isWrappedText()
	{
		return xlsStyle.getWrapText();
	}



	public short[][] getBorderRGB()
	{
		short[] cc = new short[4];
		cc[0] = xlsStyle.getTopBorderColor();
		cc[1] = xlsStyle.getRightBorderColor();
		cc[2] = xlsStyle.getBottomBorderColor();
		cc[3] = xlsStyle.getLeftBorderColor();

		short[][] ccRgb = new short[4][];
		for (int i = 0; i < cc.length; i++)
		{
			ccRgb[i] = colorToArray(cc[i], workbook);
		}	
			
		return ccRgb;
		
	}



	public short[] getBorderStyle()
	{
			short[] bb = new short[4];
			bb[0] = xlsStyle.getBorderTop();
			bb[1] = xlsStyle.getBorderRight();
			bb[2] = xlsStyle.getBorderBottom();
			bb[3] = xlsStyle.getBorderLeft();
			return bb;		
	}



	static public short[] colorToArray(short x, HSSFWorkbook workbook)
	{
		HSSFColor cc = workbook.getCustomPalette().getColor(x);
		
		
		if (cc == null)
			return null;

//		if (cc == HSSFColor.AUTOMATIC.getInstance())
//			return null;
		
		short[] rgb = cc.getTriplet();
		return rgb;
		
	}






	public short[] getFillForegroundColor()
	{
		short x = xlsStyle.getFillForegroundColor();
		return colorToArray(x, workbook);
	}



	/**
	 *
	 */

	public int getHorizontalAlignment()
	{
		return xlsStyle == null ? ALIGN_GENERAL : xlsStyle.getAlignment();
	}



	

	short[] xlsBorders()
	{
		short[] bb = new short[8];
		bb[0] = xlsStyle.getBorderTop();
		bb[1] = xlsStyle.getBorderRight();
		bb[2] = xlsStyle.getBorderBottom();
		bb[3] = xlsStyle.getBorderLeft();
		bb[4] = xlsStyle.getTopBorderColor();
		bb[5] = xlsStyle.getRightBorderColor();
		bb[6] = xlsStyle.getBottomBorderColor();
		bb[7] = xlsStyle.getLeftBorderColor();
		return bb;
	}



	public String getTextFormat()
	{
		return    workbook.createDataFormat().getFormat(xlsStyle.getDataFormat());
	}



	/* (non-Javadoc)
	 * @see org.openl.rules.table.ui.ICellStyle#getVerticalAlignment()
	 */
	public int getVerticalAlignment()
	{
		return xlsStyle == null ? ALIGN_GENERAL : xlsStyle.getVerticalAlignment();
	}
	
	
}
