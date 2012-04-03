package org.openl.util.print;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.reflect.MethodUtils;

/**
 * 
 * {@link NicePrinterAdaptor} for the beans. It will prints the bean by
 * "toString()" method if it specified and will print all bean properties
 * othewise.
 * 
 * @author PUdalau
 */
public class BeanNicePrinterAdaptor extends NicePrinterAdaptor {
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

    private Map<String, Object> getFieldMap(Object obj) {
        Map<String, Object> fieldMap = new HashMap<String, Object>();
        PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(obj.getClass());
        for (PropertyDescriptor descriptor : propertyDescriptors) {
            try {
                String propertyName = descriptor.getDisplayName();
                if (!"class".endsWith(propertyName)) {// skip field "class"
                    Object propertyValue = PropertyUtils.getProperty(obj, propertyName);
                    fieldMap.put(propertyName, propertyValue);
                }
            } catch (Exception e) {
            }
        }
        return fieldMap;
    }

    private boolean isToStringSpecified(Class<?> clazz) {
        return MethodUtils.getAccessibleMethod(clazz, "toString", new Class<?>[] {}).getDeclaringClass() != Object.class;
    }
}
