/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.ui;

/**
 * @author snshor
 *
 */
public class CellStyle implements ICellStyle
{

	public CellStyle(ICellStyle cs)
	{
		if (cs == null)
		{
			return;
		}	
		
		horizontalAlignment = cs.getHorizontalAlignment(); 

		verticalAlignment = cs.getVerticalAlignment();

		fillBackgroundColor = cs.getFillBackgroundColor();
		fillForegroundColor = cs.getFillForegroundColor();
		
		textFormat = cs.getTextFormat();
		
		borderStyle = cs.getBorderStyle();
		borderRGB = cs.getBorderRGB();

		ident = cs.getIdent();

		wrappedText = cs.isWrappedText();

		rotation = cs.getRotation();
	}
	
	
	
	int horizontalAlignment = ALIGN_GENERAL; 

	int verticalAlignment = ALIGN_GENERAL;

	short[] fillBackgroundColor;
	short[] fillForegroundColor;
	
	String textFormat;
	
	short[] borderStyle;
	short[][] borderRGB;

	int ident;

	boolean wrappedText;

	int rotation;

	public short[][] getBorderRGB()
	{
		return this.borderRGB;
	}

	public void setBorderRGB(short[][] borderRGB)
	{
		this.borderRGB = borderRGB;
	}

	public short[] getBorderStyle()
	{
		return this.borderStyle;
	}

	public void setBorderStyle(short[] borderStyle)
	{
		this.borderStyle = borderStyle;
	}

	public short[] getFillBackgroundColor()
	{
		return this.fillBackgroundColor;
	}

	public void setFillBackgroundColor(short[] fillBackgroundColor)
	{
		this.fillBackgroundColor = fillBackgroundColor;
	}

	public short[] getFillForegroundColor()
	{
		return this.fillForegroundColor;
	}

	public void setFillForegroundColor(short[] fillForegroundColor)
	{
		this.fillForegroundColor = fillForegroundColor;
	}

	public int getHorizontalAlignment()
	{
		return this.horizontalAlignment;
	}

	public void setHorizontalAlignment(int horizontalAlignment)
	{
		this.horizontalAlignment = horizontalAlignment;
	}

	public int getIdent()
	{
		return this.ident;
	}

	public void setIdent(int ident)
	{
		this.ident = ident;
	}

	public int getRotation()
	{
		return this.rotation;
	}

	public void setRotation(int rotation)
	{
		this.rotation = rotation;
	}

	public String getTextFormat()
	{
		return this.textFormat;
	}

	public void setTextFormat(String textFormat)
	{
		this.textFormat = textFormat;
	}

	public int getVerticalAlignment()
	{
		return this.verticalAlignment;
	}

	public void setVerticalAlignment(int verticalAlignment)
	{
		this.verticalAlignment = verticalAlignment;
	}

	public boolean isWrappedText()
	{
		return this.wrappedText;
	}

	public void setWrappedText(boolean wrappedText)
	{
		this.wrappedText = wrappedText;
	}


}
