package org.openl.rules.table;

public class GridRegion implements IGridRegion {
    int top, left, bottom, right;

    public GridRegion(IGridRegion g) {
        top = g.getTop();
        left = g.getLeft();
        bottom = g.getBottom();
        right = g.getRight();
    }

    public GridRegion(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }
    
    public GridRegion(IGridRegion reg, short side, int coord)
    {
    	this(reg);
    	switch(side)
    	{
    	case TOP:
    		top = coord;
    		break;
    	case LEFT:
    		left = coord;
    		break;
    	case RIGHT:
    		right = coord;
    		break;
    	case BOTTOM:
    		bottom = coord;
    		break;
    	default:
    		throw new IllegalArgumentException("Wrong IGridRegion side argument: " + side);
    	}
    }
    

    public int getBottom() {
        return bottom;
    }

    public int getLeft() {
        return left;
    }

    public int getRight() {
        return right;
    }

    public int getTop() {
        return top;
    }

    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public void setTop(int top) {
        this.top = top;
    }

}
