package org.openl.rules.dt.index;

import java.util.BitSet;
import java.util.NoSuchElementException;

import org.openl.domain.AIntIterator;

public class BitSetIterator extends AIntIterator {

    private final BitSet bitSet;
    private int currentIndex;

    /**
     * Constructs a BitSetIterator that iterates over the set bits of the specified BitSet.
     *
     * This constructor initializes the iterator by setting the current index to the first set bit
     * found at or after index 0.
     *
     * @param bitSet the BitSet to be traversed by this iterator
     */
    public BitSetIterator(BitSet bitSet) {
        this.bitSet = bitSet;
        this.currentIndex = bitSet.nextSetBit(0);
    }

    /**
     * Returns {@code true} if there is at least one additional set bit in the BitSet, indicating that the iterator has more elements.
     *
     * @return {@code true} if the iterator has additional set bits; {@code false} otherwise.
     */
    @Override
    public boolean hasNext() {
        return currentIndex >= 0;
    }

    /**
     * Returns the next set bit value as an Integer.
     * <p>
     * This method delegates to {@link #nextInt()} to retrieve the next element in the iteration.
     * If no further set bit exists, a {@code NoSuchElementException} is thrown.
     *
     * @return the next set bit value as an Integer
     * @throws NoSuchElementException if there are no more set bits to iterate over
     */
    @Override
    public Integer next() {
        return nextInt();
    }

    /**
     * Retrieves the index of the current set bit and advances the iterator to the next set bit.
     *
     * @return the index of the current set bit
     * @throws NoSuchElementException if there are no further set bits to iterate over
     */
    @Override
    public int nextInt() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        int result = currentIndex;
        currentIndex = bitSet.nextSetBit(currentIndex + 1);
        return result;
    }

    /**
     * Returns the total number of bits set to true in the BitSet.
     *
     * @return the count of set bits determined by the BitSet's cardinality.
     */
    @Override
    public int size() {
        return bitSet.cardinality();
    }

    /**
     * Indicates that this iterator supports resetting.
     *
     * @return true, as the iterator is always resetable.
     */
    @Override
    public boolean isResetable() {
        return true;
    }

    /**
     * Resets the iterator to the first set bit in the BitSet.
     *
     * <p>After this method is called, iteration will restart from the beginning of the BitSet.
     */
    @Override
    public void reset() {
        this.currentIndex = bitSet.nextSetBit(0);
    }

}
