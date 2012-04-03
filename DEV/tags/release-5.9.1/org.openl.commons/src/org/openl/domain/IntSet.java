/**
 * Created Apr 10, 2007
 */
package org.openl.domain;

import java.util.BitSet;

/**
 * @author snshor
 *
 */
public class IntSet implements IIntDomain {

    int min, max;

    BitSet bits;

    public IntSet(int min, int max) {
        this.min = min;
        this.max = max;
        bits = new BitSet(max - min + 1);
    }

    public void add(int bit) {
        bits.set(bit - min);
    }

    public boolean contains(int value) {
        return bits.get(value - min);
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    public IIntIterator intIterator() {
        return new BitSetIterator(bits, min);
    }

    public void remove(int bit) {
        bits.clear(bit - min);
    }

    public int size() {
        return bits.cardinality();
    }

}
