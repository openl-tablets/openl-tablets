package org.openl.rules.table;

import lombok.Getter;
import lombok.Setter;

public class GridRegion implements IGridRegion {

    @Getter
    @Setter
    private int top;
    @Getter
    @Setter
    private int left;
    @Getter
    @Setter
    private int bottom;
    @Getter
    @Setter
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

    @Override
    public String toString() {
        return "[" + getTop() + "," + getLeft() + "," + getBottom() + "," + getRight() + "]";
    }

    @Override
    public int hashCode() {
        var result = bottom;
        result = 31 * result + left;
        result = 31 * result + right;
        result = 31 * result + top;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        var other = (GridRegion) obj;
        if (bottom != other.bottom) {
            return false;
        }
        if (left != other.left) {
            return false;
        }
        if (right != other.right) {
            return false;
        }
        if (top != other.top) {
            return false;
        }
        return true;
    }
}
