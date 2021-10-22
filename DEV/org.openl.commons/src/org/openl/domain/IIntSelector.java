package org.openl.domain;

/**
 * @author snshor
 *
 */
public interface IIntSelector {

    final class IntSelectIterator extends AIntIterator {
        private final IIntSelector selector;
        private final IIntIterator it;
        private int next;
        private boolean hasNext = false;

        IntSelectIterator(IIntIterator it, IIntSelector selector) {
            this.it = it;
            this.selector = selector;
        }

        private void findNext() {
            while (it.hasNext()) {
                int x = it.nextInt();
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

    boolean select(int x);

}
