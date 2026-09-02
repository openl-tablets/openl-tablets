package org.openl.domain;

import lombok.RequiredArgsConstructor;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class IntArrayIterator extends AIntIterator {

    private int current;
    private final int[] ary;

    @Override
    public boolean hasNext() {
        return current < ary.length;
    }

    @Override
    public Integer next() {
        return ary[current++];
    }

    @Override
    public int nextInt() {
        return ary[current++];
    }

    @Override
    public int size() {
        return ary.length;
    }

    @Override
    public boolean isResetable() {
        return true;
    }

    @Override
    public void reset() {
        current = 0;
    }

}
