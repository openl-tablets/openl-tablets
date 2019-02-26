/*
 * Created on May 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author snshor
 * 
 */
public class CategorizedMap {

    private static class Category {
        private Category parent = null;

        int parentDistance = 0;

        private String category;

        Category(String category) {
            this.category = category;
        }

        @Override
        public boolean equals(Object obj) {
            return category.equals(((Category) obj).getCategory());
        }

        public String getCategory() {
            return category;
        }

        public Category getParent() {
            return parent;
        }

        public int getParentDistance() {
            return parentDistance;
        }

        @Override
        public int hashCode() {
            return category.hashCode();
        }

        public void setParent(Category category) {
            parent = category;
        }

        public void setParentDistance(int i) {
            parentDistance = i;
        }

    }

    private HashMap<String, Category> categories = new HashMap<String, Category>();

    protected HashMap<String, Object> all = new HashMap<String, Object>();

    private Object findByCategory(String str) {
        Category c = getCategory(str);
        while ((c = c.parent) != null) {
            Object res = all.get(c.getCategory());
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object key) {
        return getCategorized((String) key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.env.IResourceProvider#getResource(java.lang.Object)
     */
    private Object getCategorized(String category) {

        Object res = all.get(category);
        return res != null ? res : findByCategory(category);
    }

    public synchronized Category getCategory(String cc) {
        Category c = categories.get(cc);
        if (c == null) {
            c = new Category(cc);
            setParent(c);
            categories.put(cc, c);

            if (c.parent != null) {
                reassignParents(c.getParentDistance(), c.getParent());
            } else {
                reassignParents(-1, null);
            }
        }

        return c;

    }

    public Object put(Object key, Object value) {
        return putCategorized((String) key, value);
    }

    private Object putCategorized(String category, Object value) {
        if (!all.containsKey(category)) {
            getCategory(category);
        }
        return all.put(category, value);
    }

    private synchronized void reassignParents(int parentDistance, Category parent) {
        for (Category c : categories.values()) {
            if (c.getParent() == parent && c.getParentDistance() > parentDistance) {
                setParent(c);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */

    protected synchronized void setParent(Category cc) {
        String search = cc.getCategory();

        for (int i = 1;; ++i) {
            int index = search.lastIndexOf('.');
            if (index < 0) {
                break;
            }
            search = search.substring(0, index);
            Category parent = categories.get(search);
            if (parent != null) {
                cc.setParentDistance(i);
                cc.setParent(parent);
                return;
            }
        }
        cc.setParent(null);
        cc.setParentDistance(0);
    }

    public Collection<Object> values() {
        return all.values();
    }

}
