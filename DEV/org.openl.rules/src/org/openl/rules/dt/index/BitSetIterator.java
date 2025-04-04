package org.openl.rules.dt.index;

import java.util.BitSet;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.openl.domain.AIntIterator;

public class BitSetIterator extends AIntIterator {

    private final BitSet bitSet;
    private int currentIndex;

    public BitSetIterator(BitSet bitSet) {
        this.bitSet = Objects.requireNonNull(bitSet);
        this.currentIndex = bitSet.nextSetBit(0);
    }

    @Override
    public boolean hasNext() {
        return currentIndex >= 0;
    }

    @Override
    public Integer next() {
        return nextInt();
    }

    @Override
    public int nextInt() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        int result = currentIndex;
        currentIndex = bitSet.nextSetBit(currentIndex + 1);
        return result;
    }

    @Override
    public int size() {
        return bitSet.cardinality();
    }

    @Override
    public boolean isResetable() {
        return true;
    }

    @Override
    public void reset() {
        this.currentIndex = bitSet.nextSetBit(0);
    }

}
