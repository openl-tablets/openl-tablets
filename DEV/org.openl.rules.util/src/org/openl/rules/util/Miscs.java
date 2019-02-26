package org.openl.rules.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

public final class Miscs {

    private Miscs() {
        // Utility class
    }

    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        } else if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        } else if (obj instanceof Collection) {
            return ((Collection) obj).isEmpty();
        } else if (obj instanceof Map) {
            return ((Map) obj).isEmpty();
        } else if (obj instanceof CharSequence) {
            return ((CharSequence) obj).toString().trim().isEmpty();
        } else if (obj instanceof Iterable) {
            return !((Iterable) obj).iterator().hasNext();
        }
        return false;
    }

    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    public static boolean isNaN(Double num) {
        return (num != null) && Double.isNaN(num);
    }

    public static Boolean isNaN(Float num) {
        return (num != null) && Float.isNaN(num);
    }

    public static Boolean isInfinite(Double num) {
        return (num != null) && Double.isInfinite(num);
    }

    public static Boolean isInfinite(Float num) {
        return (num != null) && Float.isInfinite(num);
    }

    public static Boolean isInfinite(Long num) {
        return (num != null) && (Long.MAX_VALUE == num || Long.MIN_VALUE == num);
    }

    public static Boolean isInfinite(Integer num) {
        return (num != null) && (Integer.MAX_VALUE == num || Integer.MIN_VALUE == num);
    }
}
