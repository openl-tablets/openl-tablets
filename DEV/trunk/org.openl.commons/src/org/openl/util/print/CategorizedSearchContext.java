/**
 * Created Jan 28, 2007
 */
package org.openl.util.print;

import java.util.HashMap;
import java.util.Stack;

/**
 * @author snshor
 *
 */
public class CategorizedSearchContext implements ICategorizedSearchContext {

    private static class CKey {
        private Object key;
        private String category;

        CKey(Object key, String category) {
            this.key = key;
            this.category = category;
        }

        @Override
        public boolean equals(Object obj) {
            CKey ckey = (CKey) obj;
            return key.equals(ckey.key) && category.equals(ckey.category);
        }

        @Override
        public int hashCode() {
            return key.hashCode() + 37 * category.hashCode();
        }
    }

    private static ThreadLocal<Stack<ICategorizedSearchContext>> contexts = new ThreadLocal<Stack<ICategorizedSearchContext>>();

    private static CategorizedSearchContext defaultContext;

    private ICategorizedSearchContext parent;

    private HashMap<CKey, Object> map = new HashMap<CKey, Object>();

    public static ICategorizedSearchContext current() {
        Stack<ICategorizedSearchContext> s = contexts.get();
        if (s == null || s.size() == 0) {
            return defaultSearchContext();
        }
        return s.peek();
    }

    private static synchronized ICategorizedSearchContext defaultSearchContext() {
        if (defaultContext == null) {
            defaultContext = new CategorizedSearchContext(null);
            initDefaultContext();
        }
        return defaultContext;
    };

    private static void initDefaultContext() {
        // TODO Auto-generated method stub

    }

    public static void push(ICategorizedSearchContext cxt) {
        Stack<ICategorizedSearchContext> s = contexts.get();
        if (s == null) {
            s = new Stack<ICategorizedSearchContext>();
            contexts.set(s);
        }
        s.push(cxt);

    }

    public static void pushThis() {
        push(new CategorizedSearchContext(current()));
    }

    public CategorizedSearchContext(ICategorizedSearchContext context) {
        parent = context;
    }

    /*
     * primitive implemention, later we may improve it with key/category
     * iteration to look for things like super classes/super interfaces
     */
    public Object find(Object key, String category) {
        Object res = findLocal(key, category);
        if (res != null) {
            return res;
        }
        if (parent != null) {
            return parent.find(key, category);
        }
        return null;
    }

    public Object findLocal(Object key, String category) {
        CKey ckey = new CKey(key, category);
        return map.get(ckey);
    }

    public ICategorizedSearchContext getParent() {
        return parent;
    }

    public void register(Object key, String category, Object value) {
        map.put(new CKey(key, category), value);
    }

    public void unregister(Object key, String category) {
        map.remove(new CKey(key, category));
    }

}
