package org.openl.util;

import java.util.Collection;
import java.util.Map;

/**
 * An util class for collections and arrays.
 * 
 * @author Yury Molchan
 */
public class CollectionUtils {

    /**
     * Return {code}true{/code} if a collection is null or is empty.
     * 
     * @param col the checked collection
     * @return return {code}true{/code} if collection does not contain any
     *         elements
     * @see Collection#isEmpty()
     */
    public static boolean isEmpty(Collection<?> col) {
        return col == null || col.isEmpty();
    }

    /**
     * Return {code}true{/code} if a collection contains at least one element.
     * This method is inverse to {@link #isEmpty(Collection}.
     *
     * @param col the checked collection
     * @return {code}true{/code} if a collection contains at least one element.
     */
    public static boolean isNotEmpty(Collection<?> col) {
        return !isEmpty(col);
    }

    /**
     * Return {code}true{/code} if a map is null or is empty.
     *
     * @param map the checked collection
     * @return return {code}true{/code} if collection does not contain any
     *         elements
     * @see Map#isEmpty()
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Return {code}true{/code} if a map contains at least one element. This
     * method is inverse to {@link #isEmpty(Map)}.
     *
     * @param map the checked collection
     * @return {code}true{/code} if a collection contains at least one element.
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }
}
