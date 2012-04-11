/*
 * Created on Jun 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf.cache;

/**
 * @author snshor
 *
 */
public class CacheUtils {

    public static Object makeKey(Object... objects) {
        return new GenericKey(objects);
    }
}
