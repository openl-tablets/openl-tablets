/*
 * Created on May 3, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import java.util.BitSet;

/**
 * @author snshor
 */
public class BitSetIterator extends AIntIterator {
    int min = 0;

    BitSet bits;

    int nextBit = -1;

    boolean isNextReady = false;

    public BitSetIterator(BitSet bits) {
        this.bits = bits;
    }

    /*
     * Returns the index of the first bit that is set to <code>true</code> that
     * occurs on or after the specified starting index. If no such bit exists
     * then -1 is returned.
     * 
     * To iterate over the <code>true</code> bits in a <code>BitSet</code>, use
     * the following loop:
     * 
     * for(int i=bs.nextSetBit(0); i>=0; i=bs.nextSetBit(i+1)) { // operate on
     * index i here }
     * 
     * @param fromIndex the index to start checking from (inclusive). @return
     * the index of the next set bit. @throws IndexOutOfBoundsException if the
     * specified index is negative.
     * 
     * @since 1.4
     */

    @Override
    public boolean hasNext() {
        if (!isNextReady) {
            nextBit = bits.nextSetBit(nextBit + 1);
            isNextReady = true;
        }

        return nextBit >= 0;
    }

    /**
     *
     */

    @Override
    public int nextInt() {
        if (!isNextReady) {
            nextBit = bits.nextSetBit(nextBit + 1);
            isNextReady = true;
        }

        isNextReady = false;
        return nextBit + min;
    }

	@Override
	public boolean isResetable() {
		return true;
	}

	@Override
	public void reset() {
	    nextBit = -1;
	    isNextReady = false;
	}

}
