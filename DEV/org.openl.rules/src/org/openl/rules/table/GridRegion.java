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
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
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
        int result = bottom;
        result = 31 * result + left;
        result = 31 * result + right;
        result = 31 * result + top;
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
