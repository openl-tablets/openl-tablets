/**
 * Created Jan 28, 2007
 */
package org.openl.util.print;

/**
 * @author snshor
 *
 */
public interface ICategorizedSearchContext {

    Object find(Object key, String category);

    ICategorizedSearchContext getParent();

    void register(Object key, String category, Object value);

    void unregister(Object key, String category);

}
