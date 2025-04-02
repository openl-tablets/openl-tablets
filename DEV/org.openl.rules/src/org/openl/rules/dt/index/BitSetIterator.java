package org.openl.rules.dt.index;

import java.util.BitSet;
import java.util.NoSuchElementException;

import org.openl.domain.AIntIterator;

public class BitSetIterator extends AIntIterator {

    private final BitSet bitSet;
    private int currentIndex;

    /**
     * Constructs a BitSetIterator for the specified BitSet.
     *
     * <p>This constructor sets the iterator's current index to the first set bit in the provided BitSet,
     * enabling traversal of all set bits.
     *
     * @param bitSet the BitSet to iterate over
     */
    public BitSetIterator(BitSet bitSet) {
        this.bitSet = bitSet;
        this.currentIndex = bitSet.nextSetBit(0);
    }

    /**
     * Checks if there are more set bits available in the BitSet.
     *
     * @return {@code true} if the current index is non-negative, indicating another set bit is present; {@code false} otherwise.
     */
    @Override
    public boolean hasNext() {
        return currentIndex >= 0;
    }

    /**
     * Returns the next set bit as an Integer.
     *
     * <p>This method delegates to {@link #nextInt()} to obtain the index of the next set bit.
     * If no additional set bits are available, a {@code NoSuchElementException} is thrown.</p>
     *
     * @return the index of the next set bit as an Integer
     * @throws NoSuchElementException if there are no more set bits
     */
    @Override
    public Integer next() {
        return nextInt();
    }

    /**
     * Returns the index of the current set bit and advances the iterator to the next set bit.
     *
     * <p>If no further set bits are available, this method throws a {@link NoSuchElementException}.
     *
     * @return the index of the current set bit
     * @throws NoSuchElementException if there are no subsequent set bits
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
     * Returns the number of bits set to true in the underlying BitSet.
     *
     * @return the total count of set bits
     */
    @Override
    public int size() {
        return bitSet.cardinality();
    }

    /**
     * Indicates that this iterator supports resetting to its initial state.
     *
     * @return true, as the iterator is always resettable.
     */
    @Override
    public boolean isResetable() {
        return true;
    }

    /**
     * Resets the iterator by setting the current index to the position of the first set bit in the BitSet.
     */
    @Override
    public void reset() {
        this.currentIndex = bitSet.nextSetBit(0);
    }

}
