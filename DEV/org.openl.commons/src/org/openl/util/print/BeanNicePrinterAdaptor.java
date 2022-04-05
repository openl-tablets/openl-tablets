package org.openl.util.print;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.openl.util.ClassUtils;
import org.openl.util.NumberUtils;
import org.openl.util.StringUtils;
import org.openl.util.formatters.IFormatter;
import org.openl.util.formatters.NumberFormatter;
import org.openl.util.formatters.SmartNumberFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * {@link NicePrinterAdaptor} for the beans. It will print all bean properties.
 *
 * @author PUdalau
 */
public class BeanNicePrinterAdaptor extends NicePrinterAdaptor {
    public static final IFormatter SMART_NUMBER_FORMATTER = new SmartNumberFormatter(Locale.US);
    private static final Logger LOG = LoggerFactory.getLogger(BeanNicePrinterAdaptor. class);
    private static final Object[] EMPTY = new Object[0];

    @Override
    public void printObject(Object obj, int newID, NicePrinter printer) {
        printReference(obj, newID, printer);
        Map<String, Object> fieldMap = getFieldMap(obj);
        printMap(fieldMap, null, printer);
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
                    Object propertyValue = extractPropertyValue(obj, descriptor);
                    fieldMap.put(propertyName, propertyValue);
                }
            } catch (Exception e) {
                LOG.debug("Ignored error: ", e);
            }
        }
        return fieldMap;
    }

    private static Object extractPropertyValue(Object obj, PropertyDescriptor descriptor)
            throws IllegalAccessException, InvocationTargetException {
        Object propertyValue = descriptor.getReadMethod().invoke(obj, EMPTY);
        if (ClassUtils.isAssignable(propertyValue.getClass(), Number.class)
                && (NumberUtils.isFloatPointType(propertyValue.getClass()))) {
            propertyValue = SMART_NUMBER_FORMATTER.format(propertyValue);
        }
        return propertyValue;
    }
}
