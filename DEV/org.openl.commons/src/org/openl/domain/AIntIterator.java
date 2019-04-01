/**
 * Created Jul 14, 2007
 */
package org.openl.domain;

import org.openl.util.AOpenIterator;

/**
 * @author snshor
 *
 */
public abstract class AIntIterator extends AOpenIterator<Integer> implements IIntIterator {

    @Override
    public Integer next() {
        return nextInt();
    }

    @Override
    public IIntIterator select(IIntSelector selector) {
        return new IIntSelector.IntSelectIterator(this, selector);
    }

}
