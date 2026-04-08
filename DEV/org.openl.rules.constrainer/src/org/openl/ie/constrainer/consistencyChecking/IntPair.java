package org.openl.ie.constrainer.consistencyChecking;

public class IntPair {

    private final int x;
    private final int y;

    public IntPair(int x, int y) {
        super();
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IntPair other)) {
            return false;
        }
        if (x != other.x) {
            return false;
        }
        return y == other.y;
    }

}
