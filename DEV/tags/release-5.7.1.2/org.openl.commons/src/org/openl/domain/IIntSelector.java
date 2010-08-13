/**
 * Created Jul 14, 2007
 */
package org.openl.domain;

/**
 * @author snshor
 *
 */
public interface IIntSelector {

    static public final class IntSelectIterator extends AIntIterator {
        IIntSelector selector;
        IIntIterator it;
        int next;
        boolean hasNext = false;

        IntSelectIterator(IIntIterator it, IIntSelector selector) {
            this.it = it;
            this.selector = selector;
        }

        void findNext() {
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

        public boolean hasNext() {
            if (!hasNext) {
                findNext();
            }
            return hasNext;
        }

        public int nextInt() {
            if (!hasNext()) {
                throw new IllegalStateException();
            }
            hasNext = false;
            return next;
        }

    }

    boolean select(int x);

}
