package org.openl.rules.table;

public class GridRegion implements IGridRegion
{
	int top,left, bottom, right;

	public GridRegion(int top, int left, int bottom, int right)
	{
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
	}
	
	
	public GridRegion(IGridRegion g)
	{
		this.top = g.getTop();
		this.left = g.getLeft();
		this.bottom = g.getBottom();
		this.right = g.getRight();
	}

	
	
	public int getRight()
	{
		return right;
	}

	public void setRight(int right)
	{
		this.right = right;
	}

	public int getBottom()
	{
		return bottom;
	}

	public void setBottom(int bottom)
	{
		this.bottom = bottom;
	}

	public int getLeft()
	{
		return left;
	}

	public void setLeft(int left)
	{
		this.left = left;
	}

	public int getTop()
	{
		return top;
	}

	public void setTop(int top)
	{
		this.top = top;
	}

}
