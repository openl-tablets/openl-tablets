/*
 * Created on Jun 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.cache;

/**
 * Builds key for using it in cache.
 * 
 * @author snshor
 * 
 */
public final class CacheUtils {
    //Utils class should not have constructors.
    private CacheUtils() {
    }
    
    public static Object buildKey(Object... objects) {
        return GenericKey.getInstance(objects);
    }
    
    /**
     * Method is deprecated. Please, use buildKey method.
     * 
     * @param objects
     * @return
     */
    @Deprecated
    public static Object makeKey(Object... objects) {
        return CacheUtils.buildKey(objects);
    }
}
