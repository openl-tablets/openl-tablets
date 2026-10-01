package org.openl.domain;

import org.openl.util.AOpenIterator;

/**
 * @author snshor
 */
public abstract class AIntIterator extends AOpenIterator<Integer> implements IIntIterator {

    @Override
    public Integer next() {
        return nextInt();
    }

    @Override
    public IIntIterator select(IIntSelector selector) {
        return new IntSelectIterator(this, selector);
    }

    private static final class IntSelectIterator extends AIntIterator {
        private final IIntSelector selector;
        private final IIntIterator it;
        private int next;
        private boolean hasNext;

        IntSelectIterator(IIntIterator it, IIntSelector selector) {
            this.it = it;
            this.selector = selector;
        }

        private void findNext() {
            while (it.hasNext()) {
                var x = it.nextInt();
                if (selector.select(x)) {
                    next = x;
                    hasNext = true;
                    return;
                }
            }

            next = -1;
            hasNext = false;
        }

        @Override
        public boolean hasNext() {
            if (!hasNext) {
                findNext();
            }
            return hasNext;
        }

        @Override
        public int nextInt() {
            if (!hasNext()) {
                throw new IllegalStateException();
            }
            hasNext = false;
            return next;
        }

        @Override
        public boolean isResetable() {
            return it.isResetable();
        }

        @Override
        public void reset() {
            hasNext = false;
            it.reset();
        }

    }
}
