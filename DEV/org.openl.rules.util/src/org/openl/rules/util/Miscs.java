package org.openl.rules.util;

import java.io.File;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class Miscs {

    private Miscs() {
        // Utility class
    }

    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        } else if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        } else if (obj == Collection.class) {
            return ((Collection) obj).isEmpty();
        } else if (obj == Map.class) {
            return ((Map) obj).isEmpty();
        } else if (obj == CharSequence.class) {
            return ((CharSequence) obj).toString().trim().isEmpty();
        } else if (obj == Iterable.class) {
            return !((Iterable) obj).iterator().hasNext();
        }
        return false;
    }

    public static int length(Collection<?> array) {
        return array == null ? 0 : array.size();
    }

    public static int length(Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }

    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    public static boolean isNaN(Double num) {
        return num != null && Double.isNaN(num);
    }

    public static Boolean isNaN(Float num) {
        return num != null && Float.isNaN(num);
    }

    public static Boolean isInfinite(Double num) {
        return num != null && Double.isInfinite(num);
    }

    public static Boolean isInfinite(Float num) {
        return num != null && Float.isInfinite(num);
    }

    public static Boolean isInfinite(Long num) {
        return num != null && (Long.MAX_VALUE == num || Long.MIN_VALUE == num);
    }

    public static Boolean isInfinite(Integer num) {
        return num != null && (Integer.MAX_VALUE == num || Integer.MIN_VALUE == num);
    }

    public static <T> T copy2(T t) {
        if (t == null) {
            return null;
        }

        Class<?> clazz = t.getClass();
        if (isImmutable(clazz)) {
            return t;
        }
        Object result = null;
        if (clazz == Date.class) {
            result = new Date(((Date) t).getTime());
        }
        if (clazz.isArray()) {
            var rootClass = clazz.getComponentType();
            if (isPrimitive(rootClass) || isImmutable(rootClass)) {

                result = copyArray(t);
            }
            while (rootClass.isArray()) {
                rootClass = rootClass.getComponentType();
            }
            if (isImmutable(rootClass)) {
                result = ((Object[])t).clone();

                var x = new int[7];
                var y = ((Object[])x).clone();

            }
            new HashMap<>().clone();
            // https://github.com/aem-design/cloning/blob/master/src/main/java/com/rits/cloning/Cloner.java
        }

        if (clazz == HashMap.class) {
            result = new HashMap((HashMap) t);
        } else if (clazz == LinkedHashMap.class) {
            result = new LinkedHashMap((LinkedHashMap) t);
        } else if (clazz == TreeMap.class) {
            result = new TreeMap((TreeMap) t);
        } else if (clazz == ArrayList.class) {
            result = new ArrayList((ArrayList) t);
        }
        return (T) result;
    }

    private static Object copyArray(Object src) {
        if (src == null) {
            return null;
        }
        int length = Array.getLength(src);
        var result = Array.newInstance(src.getClass().getComponentType(), length);
        System.arraycopy(src, 0, result, 0, length);
        return result;
    }

    private static boolean isImmutable(Class<?> t) {
        return t == String.class
                || t == Double.class
                || t == Integer.class
                || t == Long.class
                || t == Boolean.class
                || t == BigDecimal.class
                || t == BigInteger.class
                || t == Character.class
                || t == Byte.class
                || t == Short.class
                || t == Float.class
                || t == Enum.class
                || t == LocalDate.class
                || t == LocalTime.class
                || t == LocalDateTime.class
                || t == ZonedDateTime.class
                || t == OffsetDateTime.class
                || t == OffsetTime.class
                || t == Duration.class
                || t == Instant.class
                || t == Period.class
                || t == Locale.class
                || t == UUID.class
                || t == URI.class
                || t == URL.class
                || t == Year.class
                || t == Month.class
                || t == YearMonth.class
                || t == MonthDay.class
                || t == DayOfWeek.class
                || t == Class.class


                ;
    }

    private static boolean isPrimitive(Class<?> t) {
        return t == double.class
                || t == int.class
                || t == long.class
                || t == boolean.class
                || t == char.class
                || t == byte.class
                || t == short.class
                || t == float.class;
    }

}
