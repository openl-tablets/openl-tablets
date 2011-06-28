/**
 * Created Jan 29, 2007
 */
package org.openl.util.print;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.openl.base.INamedThing;
import org.openl.util.OpenIterator;
import org.openl.util.StringTool;

/**
 * Default format to convert <code>Object</code> values to <code>String</code> representations.
 * Supports:<br>
 *           - <code>null</code> objects<br>
 *           - arrays (Object and primitives)<br>
 *           - object that are instances of {@link Collection}<br>
 *           - maps of objects.
 * 
 * @author snshor
 *
 */
public class DefaultFormat implements IFormat {

    public DefaultFormat() {
        super();        
    }

    public StringBuffer format(Object obj, int mode, StringBuffer buf) {
        if (obj == null) {
            return buf.append("null");
        }

        if (obj.getClass().isArray()) {
            return formatArray(obj, mode, buf);
        }

        if (obj instanceof Collection<?>) {
            return formatCollection((Collection<?>) obj, mode, buf);
        }

        if (obj instanceof Map<?,?>) {
            return formatMap((Map<?,?>) obj, mode, buf);
        }

        if (!isPrimitive(obj.getClass())) {
            return formatBean(obj, mode, buf);
        }

        return buf.append(obj);
    }

    protected StringBuffer formatArray(Object obj, int mode, StringBuffer buf) {
        int maxLen = maxCollectionLength(mode);

        return formatIterator(OpenIterator.fromArrayObj(obj), mode, buf, maxLen, Array.getLength(obj), "[]");
    }

    protected StringBuffer formatBean(Object obj, int mode, StringBuffer buf) {
        if (obj instanceof INamedThing) {
            return buf.append(((INamedThing) obj).getDisplayName(mode));
        }
        NicePrinter printer = new NicePrinter();
        printer.print(obj, new BeanNicePrinterAdaptor());
        return buf.append(printer.getBuffer());
    }

    protected StringBuffer formatCollection(Collection<?> collection, int mode, StringBuffer buf) {

        int maxLength = maxCollectionLength(mode);

        buf.append(shortClassName(collection));

        Object element = null;
        Iterator<?> it = collection.iterator();
        if (it.hasNext()) {
            element = it.next();
        }

        if (element != null) {
            buf.append('<').append(shortClassName(element)).append('>');
        }

        formatIterator(collection.iterator(), mode, buf, maxLength, collection.size(), "{}");

        return buf;
    }

    public StringBuffer formatIterator(Iterator<?> it, int mode, StringBuffer buf, int maxLength, int actualLength,
            String brackets) {
        buf.append(brackets.charAt(0));

        int len = actualLength;

        if (actualLength > maxLength + 1) {
            len = maxLength;
        }

        for (int i = 0; i < len; ++i) {
            if (i > 0) {
                buf.append(", ");
            }
            if (it.hasNext()) {
                Formatter.format(it.next(), mode, buf);
            }
        }

        if (actualLength > len) {
            buf.append(", ... " + (actualLength - len) + " more");
        }

        buf.append(brackets.charAt(1));

        return buf;
    }

    protected StringBuffer formatMap(Map<?,?> map, int mode, StringBuffer buf) {
        return formatCollection(map.keySet(), mode, buf);
    }

    protected boolean isPrimitive(Class<?> c) {
        return c.isPrimitive();
    }

    protected int maxCollectionLength(int mode) {

        switch (mode) {
            case INamedThing.SHORT:
                return 1;
            case INamedThing.REGULAR:
                return 3;
        }
        return 5;

    }

    public String shortClassName(Object obj) {
        return StringTool.lastToken(obj.getClass().getName(), ".");
    }

}
