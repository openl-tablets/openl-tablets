package org.openl.extension.xmlrules.utils;

import java.util.Date;

public class XmlRulesOperators {
    public static boolean eq(Object x, Object y) {
        if (x == y) {
            return true;
        }

        if (x == null || y == null) {
            return false;
        }

        Class expectedClass = getExpectedClass(x, y);
        x = HelperFunctions.convertArgument(expectedClass, x);
        y = HelperFunctions.convertArgument(expectedClass, y);

        return x == y || x != null && y != null && x.equals(y);
    }

    public static boolean ne(Object x, Object y) {
        return !eq(x, y);
    }

    public static boolean gt(Object x, Object y) {
        if (x == y) {
            return false;
        }

        if (x == null) {
            return false;
        }

        if (y == null) {
            return true;
        }

        Class expectedClass = getExpectedClass(x, y);
        x = HelperFunctions.convertArgument(expectedClass, x);
        y = HelperFunctions.convertArgument(expectedClass, y);

        if (x != null && y != null) {
            return ((Comparable) x).compareTo(y) > 0;
        }
        return false;
    }

    public static boolean ge(Object x, Object y) {
        if (x == y) {
            return true;
        }

        if (x == null) {
            return false;
        }

        if (y == null) {
            return true;
        }

        Class expectedClass = getExpectedClass(x, y);
        x = HelperFunctions.convertArgument(expectedClass, x);
        y = HelperFunctions.convertArgument(expectedClass, y);

        if (x == y) {
            return true;
        } else if (x != null && y != null) {
            return ((Comparable<Object>) x).compareTo(y) >= 0;
        }
        return false;
    }

    public static boolean lt(Object x, Object y) {
        return gt(y, x);
    }

    public static boolean le(Object x, Object y) {
        return ge(y, x);
    }

    private static Class getExpectedClass(Object x, Object y) {
        if (x.getClass() == y.getClass()) {
            return x.getClass();
        }

        if (x instanceof Date || y instanceof Date) {
            return Date.class;
        }

        if (x instanceof Number || y instanceof Number) {
            return Double.class;
        }

        if (x instanceof Boolean || y instanceof Boolean) {
            return Boolean.class;
        }

        if (x instanceof String || y instanceof String) {
            return String.class;
        }

        return Object.class;
    }
}
