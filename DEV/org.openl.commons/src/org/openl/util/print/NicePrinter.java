/*
 * Created on Apr 1, 2004
 *
 *
 * Developed by OpenRules, Inc. 2003, 2004
 *
 */
package org.openl.util.print;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author snshor
 * 
 */
public class NicePrinter {

    int identStep = 2;

    StringBuilder buffer = null;

    int ident = 0;

    HashMap<Object, Integer> printedObjects = new HashMap<>();

    int printedID = 0;

    public static String getTypeName(Object obj) {
        return obj.getClass().getName();
    }

    public static String print(Object obj) {
        StringBuilder buf = new StringBuilder(100);
        print(obj, buf);
        return buf.toString();
    }

    public static void print(Object obj, StringBuilder buf) {
        NicePrinter np = new NicePrinter(buf);
        np.print(obj, new NicePrinterAdaptor());
    }

    public NicePrinter() {
        this(new StringBuilder(100));
    }

    public NicePrinter(StringBuilder buf) {
        buffer = buf;
    }

    public void decIdent() {
        --ident;
    }

    /**
     * @return Returns the buffer.
     */
    public StringBuilder getBuffer() {
        return buffer;
    }

    public void incIdent() {
        ++ident;
    }

    @SuppressWarnings("unchecked")
    public void print(Object obj, NicePrinterAdaptor adaptor) {
        if (obj == null) {
            adaptor.printNull(this);
            return;
        }

        if (adaptor.isPrimitive(obj)) {
            adaptor.printPrimitive(obj, this);
            return;
        }

        Integer existingID = printedObjects.get(obj);
        if (existingID != null) {
            adaptor.printReference(obj, existingID.intValue(), this);
            return;
        }

        int newID = printedID++;

        printedObjects.put(obj, newID);

        if (obj instanceof Enum) {
            adaptor.printPrimitive(obj, this);
            return;
        }

        if (obj instanceof Map) {
            adaptor.printMap((Map<Object, Object>) obj, null, this);
            return;
        }

        if (obj instanceof Collection) {
            adaptor.printCollection((Collection<?>) obj, newID, this);
            return;
        }

        if (obj.getClass().isArray()) {
            adaptor.printArray(obj, newID, this);
            return;
        }

        adaptor.printObject(obj, newID, this);

    }

    public void startNewLine() {
        buffer.append('\n');
        for (int i = 0; i < ident; ++i) {
            for (int j = 0; j < identStep; j++) {
                buffer.append(' ');
            }
        }
    }

}
