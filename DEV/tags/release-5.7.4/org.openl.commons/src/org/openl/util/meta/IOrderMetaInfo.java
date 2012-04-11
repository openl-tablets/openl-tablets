/*
 * Created on May 6, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util.meta;

import java.util.Comparator;

/**
 * @author snshor
 */
public interface IOrderMetaInfo {

    class ComparableComparator implements Comparator<Object> {

        @SuppressWarnings("unchecked")
        public int compare(Object o1, Object o2) {
            return ((Comparable<Object>) o1).compareTo(o2);
        }

    }

    class OrderedComparator implements Comparator<Object> {
        private IOrderMetaInfo orderMetaInfo;

        public OrderedComparator(IOrderMetaInfo orderMetaInfo) {
            this.orderMetaInfo = orderMetaInfo;
        }

        public int compare(Object o1, Object o2) {
            Comparable<Object> c1 = orderMetaInfo.getOrderObject(o1);
            Comparable<Object> c2 = orderMetaInfo.getOrderObject(o2);
            return c1.compareTo(c2);
        }

    }

    Comparator<Object> DEFAULT_COMPARATOR = new ComparableComparator();

    /**
     * Produces object that is used for comparison with other objects in
     * collection
     *
     * @param obj
     * @return
     */
    Comparable<Object> getOrderObject(Object obj);

}
