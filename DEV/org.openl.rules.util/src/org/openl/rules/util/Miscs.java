package org.openl.rules.util;

import java.lang.reflect.Array;
import java.util.Collection;

public class Miscs {
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        } else if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        } else if (obj instanceof Collection) {
            return ((Collection) obj).isEmpty();
        } else {
            return false;
        }
    }

    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    public static Boolean isNaN(Double num) {
        return (num == null) ? null : Double.isNaN(num);
    }

    public static Boolean isNaN(Float num) {
        return (num == null) ? null : Float.isNaN(num);
    }

    public static Boolean isInfinite(Double num) {
        return (num == null) ? null : Double.isInfinite(num);
    }

    public static Boolean isInfinite(Float num) {
        return (num == null) ? null : Float.isInfinite(num);
    }

    public static Boolean isInfinite(Long num) {
        return (num == null) ? null : Long.MAX_VALUE == num || Long.MIN_VALUE == num;
    }

    public static Boolean isInfinite(Integer num) {
        return (num == null) ? null : Integer.MAX_VALUE == num || Integer.MIN_VALUE == num;
    }
}
