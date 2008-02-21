/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.ui;

/**
 * @author snshor
 *
 */
public class CellFont implements ICellFont
{
	
	public CellFont(ICellFont cf)
	{
		if (cf == null)
		{
			name= "arial";
			size=9;
			return;
		}	
		
		fontColor = cf.getFontColor();
		
		size = cf.getSize();
		
		name = cf.getName();
		
		italic = cf.isItalic();
		bold = cf.isBold();
		underlined = cf.isUnderlined();
		strikeout = cf.isStrikeout();
	}
	
	short[] fontColor;
	
	int size;
	
	String name;
	
	boolean italic;
	boolean bold;
	boolean underlined;
	boolean strikeout;
	
	
	
	
	public boolean isBold()
	{
		return this.bold;
	}
	public void setBold(boolean bold)
	{
		this.bold = bold;
	}
	public short[] getFontColor()
	{
		return this.fontColor;
	}
	public void setFontColor(short[] fontColor)
	{
		this.fontColor = fontColor;
	}
	public boolean isItalic()
	{
		return this.italic;
	}
	public void setItalic(boolean italic)
	{
		this.italic = italic;
	}
	public String getName()
	{
		return this.name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public int getSize()
	{
		return this.size;
	}
	public void setSize(int size)
	{
		this.size = size;
	}
	public boolean isStrikeout()
	{
		return this.strikeout;
	}
	public void setStrikeout(boolean strikeout)
	{
		this.strikeout = strikeout;
	}
	public boolean isUnderlined()
	{
		return this.underlined;
	}
	public void setUnderlined(boolean underlined)
	{
		this.underlined = underlined;
	}

}
