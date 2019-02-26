package org.openl.rules.util;

/**
 * A set of util methods to work with booleans.
 *
 * {@link #and(Boolean[])} and {@link #or(Boolean[])} methods also handle null values:
 * <ul>
 * <li>null & null = null</li>
 * <li>null & true = null</li>
 * <li>null & false = false</li>
 * <li>null | null = null</li>
 * <li>null | true = true</li>
 * <li>null | false = null</li>
 * </ul>
 * where null is unknown value (state).
 *
 * Note: For OpenL rules only! Don't use it in Java code.
 *
 * @author Yury Molchan
 */
public final class Booleans {

    private Booleans() {
        // Utility class
    }

    public static boolean allTrue(boolean[] values) {
        return Boolean.TRUE.equals(and(values));
    }

    public static boolean allTrue(Boolean[] values) {
        return Boolean.TRUE.equals(and(values));
    }

    public static boolean anyTrue(boolean[] values) {
        return Boolean.TRUE.equals(or(values));
    }

    public static boolean anyTrue(Boolean[] values) {
        return Boolean.TRUE.equals(or(values));
    }

    public static boolean allFalse(boolean[] values) {
        return Boolean.FALSE.equals(or(values));
    }

    public static boolean allFalse(Boolean[] values) {
        return Boolean.FALSE.equals(or(values));
    }

    public static boolean anyFalse(boolean[] values) {
        return Boolean.FALSE.equals(and(values));
    }

    public static boolean anyFalse(Boolean[] values) {
        return Boolean.FALSE.equals(and(values));
    }

    /* Synonyms */
    public static boolean allYes(boolean[] values) {
        return allTrue(values);
    }

    public static boolean allYes(Boolean[] values) {
        return allTrue(values);
    }

    public static boolean allNo(boolean[] values) {
        return allFalse(values);
    }

    public static boolean allNo(Boolean[] values) {
        return allFalse(values);
    }

    public static boolean anyYes(boolean[] values) {
        return anyTrue(values);
    }

    public static boolean anyYes(Boolean[] values) {
        return anyTrue(values);
    }

    public static boolean anyNo(boolean[] values) {
        return anyFalse(values);
    }

    public static boolean anyNo(Boolean[] values) {
        return anyFalse(values);
    }

    static Boolean and(boolean[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (boolean value : values) {
            if (!value) {
                return false;
            }
        }
        return true;
    }

    static Boolean and(Boolean[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        Boolean result = true;
        for (Boolean value : values) {
            if (value == null) {
                result = null;
            } else if (!value) {
                return false;
            }
        }
        return result;
    }

    static Boolean or(boolean[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (boolean value : values) {
            if (value) {
                return true;
            }
        }
        return false;
    }

    static Boolean or(Boolean[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        Boolean result = false;
        for (Boolean value : values) {
            if (value == null) {
                result = null;
            } else if (value) {
                return true;
            }
        }
        return result;
    }
}
