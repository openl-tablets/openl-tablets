package org.openl.rules.ui;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.IColorFilter;

public class CellModelDelegator implements ICellModel
{
	CellModel model;
	
	public CellModelDelegator(CellModel model)
	{
		this.model = model;
	}
	
	public boolean isReal()
	{
		return false;
	}




	public int getColspan()
	{
		return model.getColspan();
	}

	public String getContent()
	{
		return model.getContent();
	}


	public short[] getRgbBackground()
	{
		return model.getRgbBackground();
	}

	public int getRowspan()
	{
		return model.getRowspan();
	}




	public void setColspan(int colspan)
	{
		model.setColspan(colspan);
	}

	public void setContent(String content)
	{
		model.setContent(content);
	}


	public void setRgbBackground(short[] rgbBackground)
	{
		model.setRgbBackground(rgbBackground);
	}

	public void setRowspan(int rowspan)
	{
		model.setRowspan(rowspan);
	}

	public void toHtmlString(StringBuffer buf, TableModel table)
	{
		model.toHtmlString(buf, table);
	}

	public ICellFont getFont()
	{
		return model.getFont();
	}

	public void setFont(ICellFont font)
	{
		model.setFont(font);
	}

	/* (non-Javadoc)
	 * @see org.openl.rules.ui.ICellModel#setColorFilter(org.openl.rules.ui.IColorFilter[])
	 */
	public void setColorFilter(IColorFilter[] filter) {
		// TODO Auto-generated method stub
		
	}

	public BorderStyle[] getBorderStyle()
	{
		return this.model.getBorderStyle();
	}

	public void setBorderStyle(BorderStyle[] borderStyle)
	{
		this.model.setBorderStyle(borderStyle);
	}

	public CellModel getModel()
	{
		return this.model;
	}
	
}
