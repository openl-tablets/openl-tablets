package org.openl.rules.table;

public class GridRegion implements IGridRegion {

    private int top;
    private int left;
    private int bottom;
    private int right;

    public GridRegion(IGridRegion g) {
        top = g.getTop();
        left = g.getLeft();
        bottom = g.getBottom();
        right = g.getRight();
    }

    public GridRegion(int top, int left, int bottom, int right) {
        //TableParts change
        /*if (bottom < top) {
            throw new IllegalArgumentException("The bottom row (" + bottom + ") must not be less than the top row("
                    + top + ").");
        }
        if (right < left) {
            throw new IllegalArgumentException("The right row (" + right + ") must not be less than the left row("
                    + left + ").");
        }*/
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    public GridRegion(IGridRegion reg, short side, int coord) {
        this(reg);
        switch (side) {
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

    @Override
    public String toString() {
        return  "[" + getTop() + "," + getLeft() + "," + getBottom() + "," + getRight() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + bottom;
        result = prime * result + left;
        result = prime * result + right;
        result = prime * result + top;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GridRegion other = (GridRegion) obj;
        if (bottom != other.bottom)
            return false;
        if (left != other.left)
            return false;
        if (right != other.right)
            return false;
        if (top != other.top)
            return false;
        return true;
    }

}
