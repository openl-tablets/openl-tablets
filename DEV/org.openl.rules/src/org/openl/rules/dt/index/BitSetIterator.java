package org.openl.rules.dt.index;

import java.util.BitSet;
import java.util.NoSuchElementException;

import org.openl.domain.AIntIterator;

public class BitSetIterator extends AIntIterator {

    private final BitSet bitSet;
    private int currentIndex;

    /**
     * Constructs a BitSetIterator for iterating over the set bits of the provided BitSet.
     * <p>
     * This constructor initializes the iterator by setting the current index to the first set bit
     * (if any) found in the BitSet, starting at index 0.
     *
     * @param bitSet the BitSet whose set bits will be traversed
     */
    public BitSetIterator(BitSet bitSet) {
        this.bitSet = bitSet;
        this.currentIndex = bitSet.nextSetBit(0);
    }

    /**
     * Returns {@code true} if there is another set bit available in the BitSet.
     *
     * @return {@code true} if the iterator has more set bits; {@code false} otherwise.
     */
    @Override
    public boolean hasNext() {
        return currentIndex >= 0;
    }

    /**
     * Retrieves the next set bit's index as an Integer.
     *
     * @return the index of the next set bit
     * @throws NoSuchElementException if no more set bits are available
     */
    @Override
    public Integer next() {
        return nextInt();
    }

    /**
     * Returns the index of the next set bit from the associated BitSet.
     *
     * <p>This method retrieves the current set bit index, advances the iterator to the next set bit,
     * and returns the original index. If there are no further set bits, a {@link NoSuchElementException}
     * is thrown.
     *
     * @return the index of the current set bit before advancing to the next one
     * @throws NoSuchElementException if no subsequent set bit exists
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
     * Returns the total number of set bits in the underlying BitSet.
     *
     * @return the count of set bits in the BitSet
     */
    @Override
    public int size() {
        return bitSet.cardinality();
    }

    /**
     * Indicates whether the iterator can be reset.
     *
     * @return {@code true} since this iterator supports resetting.
     */
    @Override
    public boolean isResetable() {
        return true;
    }

    /**
     * Resets the iterator to the beginning of the BitSet.
     *
     * <p>This method reinitializes the iterator's state by setting the current index to the first set bit
     * in the BitSet, allowing iteration to start over.
     */
    @Override
    public void reset() {
        this.currentIndex = bitSet.nextSetBit(0);
    }

}
