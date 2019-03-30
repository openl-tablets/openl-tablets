package org.openl.util;

import java.util.Iterator;

import org.openl.domain.IDomain;

public final class DomainUtils {

    private DomainUtils() {
    }

    @SuppressWarnings("unchecked")
    public static String toString(@SuppressWarnings("rawtypes") IDomain domain) {
        StringBuilder sb = new StringBuilder();
        Iterator<Object> itr = domain.iterator();
        boolean f = false;
        while (itr.hasNext() && sb.length() < 200) {
            Object v = itr.next();
            if (f) {
                sb.append(", ");
            } else {
                f = true;
            }
            sb.append(v.toString());
        }
        if (itr.hasNext()) {
            sb.append(", ...");
        }
        return "[" + sb.toString() + "]";
    }

}
