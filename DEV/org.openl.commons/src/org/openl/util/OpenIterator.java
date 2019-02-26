/*
 * Created on Jun 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Iterator;

/**
 * @author snshor
 * 
 */
public class OpenIterator<T> extends AOpenIterator.SimpleIteratorWrapper<T> {

    public static <T> Iterator<T> fromArray(T[] ary) {
        if (ary == null || ary.length == 0) {
            return empty();
        }

        return new AIndexedIterator.ArrayIterator<T>(ary);
    }

    public static Iterator<Object> fromArrayObj(Object ary) {
        if (ary == null) {
            return empty();
        }

        return new AIndexedIterator.AnyArrayIterator(ary);
    }

    public OpenIterator(Iterator<T> it) {
        super(it);
    }
}
