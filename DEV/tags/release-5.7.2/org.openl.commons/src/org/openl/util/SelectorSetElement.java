/**
 * Created Apr 24, 2007
 */
package org.openl.util;

/**
 * @author snshor
 *
 */

public class SelectorSetElement {
    static public final String OR = "OR", AND = "AND";

    boolean isNot = false;

    String andOr = AND;

    protected ISelector<Object> selector;

    static public boolean select(Object obj, SelectorSetElement[] elements) {
        boolean res = true;

        for (int i = 0; i < elements.length; i++) {
            boolean e = elements[i].select(obj);
            if (elements[i].andOr.equals(OR)) {
                if (res == true && i != 0) {
                    return true;
                }
                res = e;
            } else {
                res &= e;
            }
        }
        return res;
    }

    public String getAndOr() {
        return andOr;
    }

    public ISelector<Object> getSelector() {
        return selector;
    }

    public boolean isNot() {
        return isNot;
    }

    boolean select(Object obj) {
        return isNot ? !selector.select(obj) : selector.select(obj);
    }

    public void setAndOr(String andOr) {
        this.andOr = andOr;
    }

    public void setNot(boolean isNot) {
        this.isNot = isNot;
    }

    public void setSelector(ISelector<Object> selector) {
        this.selector = selector;
    }
}
