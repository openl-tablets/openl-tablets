/**
 * Created Jan 29, 2007
 */
package org.openl.util.print;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.openl.base.INamedThing;
import org.openl.util.ClassUtils;
import org.openl.util.OpenIterator;

/**
 * Default format to convert <code>Object</code> values to <code>String</code> representations. Supports:<br>
 * - <code>null</code> objects<br>
 * - arrays (Object and primitives)<br>
 * - object that are instances of {@link Collection}<br>
 * - maps of objects.
 *
 * @author snshor
 *
 */
public final class DefaultFormat {

    private DefaultFormat() {
    }

    public static StringBuilder format(Object obj, StringBuilder buf) {
        if (obj == null) {
            return buf.append("null");
        }

        if (obj.getClass().isArray()) {
            return formatArray(obj, buf);
        }

        if (obj instanceof Collection<?>) {
            return formatCollection((Collection<?>) obj, buf);
        }

        if (obj instanceof Map<?, ?>) {
            return formatMap((Map<?, ?>) obj, buf);
        }
        if (obj instanceof Map.Entry<?, ?>) {
            return formatMapEntry((Map.Entry<?, ?>) obj, buf);
        }
        if (!obj.getClass().isPrimitive()) {
            return formatBean(obj, buf);
        }

        return buf.append(obj);
    }

    private static StringBuilder formatMapEntry(Map.Entry<?, ?> obj, StringBuilder buf) {
        buf.append("(");
        format(obj.getKey(), buf);
        buf.append(" : ");
        format(obj.getValue(), buf);
        buf.append(")");
        return buf;
    }

    protected static StringBuilder formatArray(Object obj, StringBuilder buf) {

        return formatIterator(OpenIterator.fromArrayObj(obj), buf, Array.getLength(obj), "[]");
    }

    protected static StringBuilder formatBean(Object obj, StringBuilder buf) {
        if (obj instanceof INamedThing) {
            return buf.append(((INamedThing) obj).getDisplayName(INamedThing.REGULAR));
        }
        NicePrinter printer = new NicePrinter();
        printer.print(obj, new BeanNicePrinterAdaptor());
        return buf.append(printer.getBuffer());
    }

    protected static StringBuilder formatCollection(Collection<?> collection, StringBuilder buf) {

        buf.append(ClassUtils.getShortClassName(collection.getClass()));

        Object element = null;
        Iterator<?> it = collection.iterator();
        if (it.hasNext()) {
            element = it.next();
        }

        if (element != null) {
            buf.append('<').append(ClassUtils.getShortClassName(element.getClass())).append('>');
        }

        formatIterator(collection.iterator(), buf, collection.size(), "{}");

        return buf;
    }

    private static StringBuilder formatIterator(Iterator<?> it, StringBuilder buf, int actualLength, String brackets) {
        buf.append(brackets.charAt(0));

        int len = actualLength;

        if (actualLength > 4) {
            len = 3;
        }

        for (int i = 0; i < len; ++i) {
            if (i > 0) {
                buf.append(", ");
            }
            if (it.hasNext()) {
                format(it.next(), buf);
            }
        }

        if (actualLength > len) {
            buf.append(", ... ").append(actualLength - len).append(" more");
        }

        buf.append(brackets.charAt(1));

        return buf;
    }

    protected static StringBuilder formatMap(Map<?, ?> map, StringBuilder buf) {

        buf.append(ClassUtils.getShortClassName(map.getClass()));

        Map.Entry<?, ?> element = null;
        Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
        if (it.hasNext()) {
            element = it.next();
        }

        if (element != null) {
            Object key = element.getKey();
            Object value = element.getValue();
            if (key != null && value != null) {
                buf.append('<')
                    .append(ClassUtils.getShortClassName(key.getClass()))
                    .append(',')
                    .append(ClassUtils.getShortClassName(value.getClass()))
                    .append('>');
            }
        }

        formatIterator(map.entrySet().iterator(), buf, map.size(), "{}");

        return buf;
    }

}
