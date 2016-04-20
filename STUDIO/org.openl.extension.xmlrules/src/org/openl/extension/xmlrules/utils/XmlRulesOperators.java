package org.openl.extension.xmlrules.utils;

import java.util.Date;

import org.openl.binding.impl.Operators;

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

        return Operators.eq(x, y);
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

        return Operators.strict_gt((Comparable) x, (Comparable) y);
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

        return Operators.strict_ge((Comparable) x, (Comparable) y);
    }

    public static boolean lt(Object x, Object y) {
        if (x == y) {
            return false;
        }

        if (x == null) {
            return true;
        }

        if (y == null) {
            return false;
        }

        Class expectedClass = getExpectedClass(x, y);
        x = HelperFunctions.convertArgument(expectedClass, x);
        y = HelperFunctions.convertArgument(expectedClass, y);

        return Operators.strict_lt((Comparable) x, (Comparable) y);
    }

    public static boolean le(Object x, Object y) {
        if (x == y) {
            return true;
        }

        if (x == null) {
            return true;
        }

        if (y == null) {
            return false;
        }

        Class expectedClass = getExpectedClass(x, y);
        x = HelperFunctions.convertArgument(expectedClass, x);
        y = HelperFunctions.convertArgument(expectedClass, y);

        return Operators.strict_le((Comparable) x, (Comparable) y);
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
