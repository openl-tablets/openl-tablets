package org.openl.util.filter;

/**
 * <code>OpenLFilter</code> that accepts all the objects.
 *
 * @author Aliaksandr Antonik.
 */
public class AllOpenLFilter extends BaseOpenLFilter {
    public final static AllOpenLFilter INSTANCE = new AllOpenLFilter();

    /**
     * Returns <code>true</code> for any object, including <code>null</code>.
     *
     * @param obj object to check
     * @return true
     */
    public boolean select(Object obj) {
        return true;
    }

    /**
     * Returns <code>true</code> for any class, including <code>null</code>.
     *
     * @param aClass a class to check
     * @return true
     */
    public boolean supports(Class<?> aClass) {
        return true;
    }
}
