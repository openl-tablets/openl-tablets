package org.openl.config;

import java.io.InputStream;

/**
 * @author Aleh Bykhavets
 */
public abstract class ConfigLocator implements Comparable<ConfigLocator> {
    /** priority, the higher the more important */
    private int priority;

    /**
     * Note: Reversed compare.
     * <p>
     * ConfigLocator with higher priority must be checked first.
     * 
     * @param o other ConfigLocator
     * @return reversed compare
     */
    public int compareTo(ConfigLocator o) {
        // 5, 4, 3, 2, 1
        return -(getPriority() - o.getPriority());
    }

    /**
     * Returns current priority of the ConfigLocator.
     * <p>
     * Locator with higher priority value will be checked first.
     * 
     * @return priority value
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Locate resource that contains config data.
     * <p>
     * If resource cannot be found this method returns <code>null</code>.
     * 
     * @param fullName full name of resource
     * @return content or <code>null</code>
     */
    public abstract InputStream locate(String fullName);

    /**
     * Set priority of the ConfigLocator.
     * <p>
     * It is not advised to change priority on-the-fly since it will affect
     * other users.
     * <p>
     * Negative values are allowed too.
     * 
     * @param priority new priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }
}
