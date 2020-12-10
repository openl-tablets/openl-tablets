package org.openl.util.print;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * {@link NicePrinterAdaptor} for the beans. It will prints the bean by "toString()" method if it specified and will
 * print all bean properties othewise.
 *
 * @author PUdalau
 */
public class BeanNicePrinterAdaptor extends NicePrinterAdaptor {
    private static final Logger LOG = LoggerFactory.getLogger(BeanNicePrinterAdaptor. class);
    private static final Object[] EMPTY = new Object[0];

    @Override
    public void printObject(Object obj, int newID, NicePrinter printer) {
        if (isToStringSpecified(obj.getClass())) {
            super.printObject(obj, newID, printer);
        } else {
            printReference(obj, newID, printer);
            Map<String, Object> fieldMap = getFieldMap(obj);
            printMap(fieldMap, null, printer);
        }
    }

    private static Map<String, Object> getFieldMap(Object obj) {
        final PropertyDescriptor[] propertyDescriptors;
        try {
            propertyDescriptors = Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors();
        } catch (Exception e) {
            LOG.debug("Ignored error: ", e);
            return Collections.emptyMap();
        }
        Map<String, Object> fieldMap = new HashMap<>();
        for (PropertyDescriptor descriptor : propertyDescriptors) {
            try {
                String propertyName = descriptor.getDisplayName();
                if (!"class".endsWith(propertyName)) {// skip field "class"
                    Object propertyValue = descriptor.getReadMethod().invoke(obj, EMPTY);
                    fieldMap.put(propertyName, propertyValue);
                }
            } catch (Exception e) {
                LOG.debug("Ignored error: ", e);
            }
        }
        return fieldMap;
    }

    private static boolean isToStringSpecified(Class<?> clazz) {
        try {
            return clazz.getMethod("toString", new Class<?>[] {}).getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
