/*
 * Created on Jul 18, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author snshor
 *
 */
public interface IOpenIteratorExtender<E, T> {

    final class CollectionExtender<E> implements IOpenIteratorExtender<E, Collection<E>> {

        public Iterator<E> extend(Collection<E> col) {
            return col.iterator();
        }

    }

    CollectionExtender<Object> COLLECTION_EXTENDER = new CollectionExtender<Object>();

    Iterator<E> extend(T obj);

}
