package org.openl.rules.cmatch.matcher;

public interface IMatcher {
    /**
     * Parse matching object from string.
     * <p>
     * Note that type of return value can depend on format of checkValue. For example, it can be Integer or IntRange.
     *
     * @param checkValue string value of a cell
     * @return matching value
     */
    Object fromString(String checkValue);

    /**
     * Check whether actual value and check value are match or satisfy matching operation.
     *
     * @param var actual value
     * @param checkValue check value
     * @return true if it matches
     */
    boolean match(Object var, Object checkValue);
}
