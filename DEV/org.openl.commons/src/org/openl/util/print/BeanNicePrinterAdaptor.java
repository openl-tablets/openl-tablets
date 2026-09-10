package org.openl.util.print;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link NicePrinterAdaptor} for the beans. It will prints the bean by "toString()" method if it specified and will
 * print all bean properties othewise.
 *
 * @author PUdalau
 */
@Slf4j
public class BeanNicePrinterAdaptor extends NicePrinterAdaptor {
    private static final Object[] EMPTY = new Object[0];

    @Override
    public void printObject(Object obj, int newID, NicePrinter printer) {
        if (isToStringSpecified(obj.getClass())) {
            super.printObject(obj, newID, printer);
        } else {
            printReference(obj, newID, printer);
            var fieldMap = getFieldMap(obj);
            printMap(fieldMap, null, printer);
        }
    }

    private static Map<String, Object> getFieldMap(Object obj) {
        final PropertyDescriptor[] propertyDescriptors;
        try {
            propertyDescriptors = Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors();
        } catch (Exception e) {
            log.debug("Ignored error: ", e);
            return Map.of();
        }
        var fieldMap = new HashMap<String, Object>();
        for (PropertyDescriptor descriptor : propertyDescriptors) {
            try {
                var propertyName = descriptor.getDisplayName();
                if (!"class".endsWith(propertyName)) {// skip field "class"
                    var propertyValue = descriptor.getReadMethod().invoke(obj, EMPTY);
                    fieldMap.put(propertyName, propertyValue);
                }
            } catch (Exception e) {
                log.debug("Ignored error: ", e);
            }
        }
        return fieldMap;
    }

    private static boolean isToStringSpecified(Class<?> clazz) {
        try {
            return clazz.getMethod("toString").getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
