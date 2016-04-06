package org.openl.extension.xmlrules.utils;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.DateUtil;
import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.Type;

public class HelperFunctions {
    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static Pattern DEFAULT_DATE_PATTERN = Pattern.compile("(\\d{4})[-/\\.](\\d{1,2})[-/\\.](\\d{1,2})(\\s+(\\d{1,2}):(\\d{1,2})(:(\\d{1,2})(\\.(\\d+))?)?)?");
    private static Pattern US_DATE_PATTERN = Pattern.compile("(\\d{1,2})[-/\\.](\\d{1,2})[-/\\.](\\d{4})(\\s+(\\d{1,2}):(\\d{1,2})(:(\\d{1,2})(\\.(\\d+))?)?)?");

    public static <T> T[][] transpose(T[][] arr) {
        if (arr == null || arr.length == 0) {
            return arr;
        }

        Class clazz = arr[0].getClass().getComponentType();
        @SuppressWarnings("unchecked")
        T[][] newArr = (T[][]) Array.newInstance(clazz, arr[0].length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            T[] row = arr[i];
            for (int j = 0; j < row.length; j++) {
                newArr[j][i] = row[j];
            }
        }
        return newArr;
    }

    public static Double toDouble(Object x) {
        if (x == null) {
            return null;
        }

        if (x instanceof Double) {
            return (Double) x;
        }

        if (x instanceof String) {
            return Double.valueOf((String) x);
        }

        // Other number types
        if (x instanceof Number) {
            return ((Number) x).doubleValue();
        }

        if (x.getClass().isArray() && Array.getLength(x) == 1) {
            return toDouble(Array.get(x, 0));
        }

        throw new IllegalArgumentException("Can't convert to double");
    }

    public static Integer toInteger(Object x) {
        if (x == null) {
            return null;
        }

        if (x instanceof Integer) {
            return (Integer) x;
        }

        if (x instanceof String) {
            return Integer.valueOf((String) x);
        }

        // Other number types
        if (x instanceof Number) {
            return ((Number) x).intValue();
        }

        if (x.getClass().isArray() && Array.getLength(x) == 1) {
            return toInteger(Array.get(x, 0));
        }

        throw new IllegalArgumentException("Can't convert to integer");
    }

    public static Boolean toBoolean(Object x) {
        if (x == null) {
            return null;
        }

        if (x instanceof Boolean) {
            return (Boolean) x;
        }

        if (x instanceof String) {
            return Boolean.valueOf((String) x);
        }

        // Other number types
        if (x instanceof Number) {
            return ((Number) x).intValue() == 0;
        }

        if (x.getClass().isArray() && Array.getLength(x) == 1) {
            return toBoolean(Array.get(x, 0));
        }

        throw new IllegalArgumentException("Can't convert to integer");
    }

    public static Object convertArgument(Class<?> expectedClass, Object value) {
        if (value != null) {
            Class<?> valueClass = value.getClass();
            if (!expectedClass.isAssignableFrom(valueClass)) {
                if (expectedClass.isArray()) {
                    Class<?> componentType = expectedClass.getComponentType();
                    if (valueClass.isArray()) {
                        // For example expected: Rider[], but actual: Object[] with Rider objects
                        int size = Array.getLength(value);

                        Object newValue = Array.newInstance(componentType, size);
                        for (int i = 0; i < size; ++i) {
                            Array.set(newValue, i, convertArgument(componentType, Array.get(value, i)));
                        }

                        value = newValue;
                    } else {
                        Object newValue = Array.newInstance(componentType, 1);
                        Array.set(newValue, 1, convertArgument(componentType, value));
                        value = newValue;
                    }
                } else if (valueClass.isArray()) {
                    if (Array.getLength(value) == 1) {
                        value = convertArgument(expectedClass, Array.get(value, 0));
                    }
                } else if (Double.class == expectedClass) {
                    value = toDouble(value);
                } else if (Integer.class == expectedClass) {
                    value = toInteger(value);
                } else if (Boolean.class == expectedClass) {
                    value = toBoolean(value);
                } else if (Date.class == expectedClass) {
                    value = toDate(value);
                } else if (String.class == expectedClass) {
                    if (value instanceof Date) {
                        value = DEFAULT_DATE_FORMAT.format(value);
                    } else if (value instanceof Number || value instanceof Boolean){
                        value = String.valueOf(value);
                    } else {
                        throw new ClassCastException("Can't convert argument from '" + value.getClass().getSimpleName() + "' to '" + expectedClass.getSimpleName() + "'");
                    }
                }
            }
        }
        return value;
    }

    public static Date toDate(Object date) {
        if (date == null || date instanceof Date) {
            return (Date) date;
        }

        return getCalendar(date).getTime();
    }

    public static Calendar getCalendar(Object date) {
        if (date instanceof Double) {
            return DateUtil.getJavaCalendar((Double) date);
        } else if (date instanceof Integer) {
            return DateUtil.getJavaCalendar((Integer) date);
        } else if (date instanceof String) {
            try {
                return DateUtil.getJavaCalendar(Double.parseDouble((String) date));
            } catch (NumberFormatException e) {
                Matcher matcher = DEFAULT_DATE_PATTERN.matcher((CharSequence) date);
                if (!matcher.matches()) {
                    matcher = US_DATE_PATTERN.matcher((CharSequence) date);
                }
                if (matcher.matches()) {
                    Calendar calendar = new GregorianCalendar();
                    calendar.set(Calendar.YEAR, Integer.parseInt(matcher.group(1)));
                    calendar.set(Calendar.MONTH, Integer.parseInt(matcher.group(2)) - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(matcher.group(3)));

                    String hour = matcher.group(5);
                    calendar.set(Calendar.HOUR_OF_DAY, hour != null ? Integer.parseInt(hour) : 0);
                    String minute = matcher.group(6);
                    calendar.set(Calendar.MINUTE, minute != null ? Integer.parseInt(minute) : 0);
                    String second = matcher.group(8);
                    calendar.set(Calendar.SECOND, second != null ? Integer.parseInt(second) : 0);
                    String millisecond = matcher.group(10);
                    calendar.set(Calendar.MILLISECOND, millisecond != null ? Integer.parseInt(millisecond) : 0);
                    return calendar;
                }
            }
        } else if (date instanceof Date) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime((Date) date);
            return calendar;
        } else if (date instanceof String[][]) {
            String[][] dateArray = (String[][]) date;
            if (dateArray.length > 0 && dateArray[0].length > 0) {
                return getCalendar(dateArray[0][0]);
            }
        }

        throw new IllegalArgumentException("Unsupported date format '" + date + "'");
    }

    /**
     * Get OpenL type analogue for XmlRules type
     */
    public static String getOpenLType(String xmlRulesType) {
        // Number is Double. Other type names are same
        if ("Number".equals(xmlRulesType)) {
            return "Double";
        }

        Type type = ProjectData.getCurrentInstance().getType(xmlRulesType);
        if (type != null) {
            // To handle case insensitivity issues
            return type.getName();
        }

        return xmlRulesType;
    }

    /**
     * Convert XmlRules type to OpenL type
     */
    public static String convertToOpenLType(String xmlRulesType) {
        String openLType = getOpenLType(xmlRulesType);
        if (ProjectData.getCurrentInstance().containsType(openLType)) {
            // TODO: Remove it when it will be possible to choose in LE, if the type is an array
            openLType += "[]";
        }

        return  openLType;
    }
}
