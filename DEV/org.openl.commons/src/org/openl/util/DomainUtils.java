package org.openl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.openl.domain.IDomain;

public final class DomainUtils {

    private DomainUtils() {
    }

    /** The values of a domain, each as the text it is written as; the words a vocabulary datatype allows. */
    public static String[] values(IDomain<?> domain) {
        var values = new ArrayList<String>();
        for (Object value : domain) {
            values.add(String.valueOf(value));
        }
        return values.toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    public static String toString(@SuppressWarnings("rawtypes") IDomain domain) {
        var sb = new StringBuilder();
        Iterator<Object> itr = domain.iterator();
        var f = false;
        while (itr.hasNext() && sb.length() < 200) {
            var v = itr.next();
            if (f) {
                sb.append(", ");
            } else {
                f = true;
            }
            if (v.getClass().isArray()) {
                sb.append(Arrays.deepToString((Object[]) v));
            } else {
                sb.append(v);
            }
        }
        if (itr.hasNext()) {
            sb.append(", ...");
        }
        return "[" + sb + "]";
    }

}
