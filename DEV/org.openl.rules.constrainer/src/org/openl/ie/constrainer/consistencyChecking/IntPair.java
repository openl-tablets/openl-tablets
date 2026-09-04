package org.openl.ie.constrainer.consistencyChecking;

import lombok.Getter;

public class IntPair {

    @Getter
    private final int x;
    @Getter
    private final int y;

    public IntPair(int x, int y) {
        super();
        this.x = x;
        this.y = y;
    }

    @Override
    public int hashCode() {
        final var prime = 31;
        var result = 1;
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
