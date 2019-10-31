/*
 * Created on Apr 1, 2004
 *
 *
 * Developed by OpenRules, Inc. 2003, 2004
 *
 */
package org.openl.util.print;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.*;

import org.openl.util.ClassUtils;

/**
 * @author snshor
 *
 */
public class NicePrinterAdaptor {

    static class MapEntryComparator<K, V> implements Comparator<Map.Entry<K, V>> {

        @Override
        public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {

            String key1 = String.valueOf(e1.getKey());
            String key2 = String.valueOf(e2.getKey());

            if (key1.equals("id")) {
                return -1;
            }

            if (key2.equals("id")) {
                return 1;
            }

            if (key1.equals("name")) {
                return -1;
            }

            if (key2.equals("name")) {
                return 1;
            }

            return key1.compareTo(key2);

        }

    }

    static Class<?>[] primitiveClasses = { Integer.class,
            Double.class,
            Boolean.class,
            Character.class,
            Float.class,
            Byte.class,
            Long.class,
            Short.class,
            String.class,
            Date.class };

    public static final Comparator<Map.Entry<Object, Object>> mapComparator = new MapEntryComparator<>();

    public static boolean isPrimitiveClass(Class<?> c) {

        for (int i = 0; i < primitiveClasses.length; i++) {
            if (primitiveClasses[i] == c) {
                return true;
            }
        }

        return false;
    }

    public static String shortTypeName(String classname) {

        int idx = classname.lastIndexOf('.');

        return idx < 0 ? classname : classname.substring(idx + 1);
    }

    public Object getProperty(Object obj, String propertyName) {
        try {
            Method m = obj.getClass().getMethod(ClassUtils.getter(propertyName));
            return m.invoke(obj);
        } catch (Exception t) {
            return null;
        }
    }

    protected String getTypeName(Object obj) {
        return obj.getClass().getTypeName();
    }

    public Object getUniqueID(Object obj) {
        Object id = getProperty(obj, "name");
        if (id == null) {
            id = getProperty(obj, "id");
        }
        return id;
    }

    public boolean isPrimitive(Object obj) {
        return isPrimitiveClass(obj.getClass());
    }

    public void printArray(Object ary, int newID, NicePrinter printer) {
        int len = Array.getLength(ary);

        if (len == 0) {
            printer.getBuffer().append("[]");
            return;
        }

        printer.getBuffer().append('{');
        printer.incIdent();

        for (int i = 0; i < len; i++) {
            printer.startNewLine();
            printer.getBuffer().append('[').append(i).append("]=");
            printer.print(Array.get(ary, i), this);
        }
        printer.startNewLine();
        printer.getBuffer().append('}');

        printer.decIdent();

    }

    public void printCollection(Collection<?> c, int newID, NicePrinter printer) {
        Object[] ary = new Object[c.size()];
        Iterator<?> it = c.iterator();
        for (int i = 0; it.hasNext(); i++) {
            ary[i] = it.next();
        }
        printArray(ary, newID, printer);
    }

    @SuppressWarnings("unchecked")
    public void printMap(Map map, Comparator<Map.Entry<Object, Object>> mapEntryComparator, NicePrinter printer) {
        int len = map.size();

        if (len == 0) {
            printer.getBuffer().append("[]");
            return;
        }
        Map.Entry<Object, Object>[] entries = new Map.Entry[len];
        Iterator<Map.Entry<Object, Object>> it = map.entrySet().iterator();
        for (int i = 0; it.hasNext(); i++) {
            entries[i] = it.next();
        }

        if (mapEntryComparator == null) {
            mapEntryComparator = mapComparator;
        }

        Arrays.sort(entries, mapEntryComparator);

        printer.getBuffer().append('{');
        printer.incIdent();

        for (int i = 0; i < len; i++) {
            printer.startNewLine();
            printer.getBuffer().append(entries[i].getKey()).append("=");
            printer.print(entries[i].getValue(), this);
        }
        printer.startNewLine();
        printer.getBuffer().append('}');

        printer.decIdent();

    }

    public void printNull(NicePrinter printer) {
        printer.getBuffer().append("null");
    }

    public void printObject(Object obj, int newID, NicePrinter printer) {
        printer.getBuffer().append(obj);
    }

    public void printPrimitive(Object obj, NicePrinter printer) {
        if (obj.getClass() == Double.class) {
            printer.getBuffer().append(printDouble(((Double) obj)));
        } else {
            printer.getBuffer().append(obj);
        }
    }

    public void printReference(Object obj, int id, NicePrinter printer) {
        printer.getBuffer().append(shortTypeName(getTypeName(obj)));

        Object objID = getUniqueID(obj);
        if (objID == null) {
            objID = String.valueOf(id);
        }
        printer.getBuffer().append('(').append("id=").append(objID).append(')');
    }

    private static String printDouble(double dd) {
        double d = dd < 0 ? -dd : dd;

        double x = 1;

        for (int i = 0; i < 7; i++) {
            if (d > x) {

                NumberFormat nf = NumberFormat.getNumberInstance();

                nf.setMinimumFractionDigits(0);
                nf.setMaximumFractionDigits(2 + i);

                return nf.format(dd);
            }
            x /= 10;
        }

        return String.valueOf(dd);

    }
}
