package org.openl.rules.liveexcel;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Mock API to interact with service model.
 * 
 * @author PUdalau
 */
public abstract class ServiceModelAPI {

    /**
     * @return List of all UDFs defined by service model.
     */
    public abstract List<String> getAllServiceModelUDFs();

    /**
     * @param name Name of domain type.
     * @return Class of domain type.
     */
    public Class<?> getServiceModelObjectDomainType(String name) {
        return null;
    }

    /**
     * @param name Name of property to get.
     * @param object Object that contains specified property.
     * @return Value of property of object if its exists.
     */
    public Object getValue(String name, Object object) {
        try {
            Method method = object.getClass().getMethod("get" + name.toUpperCase(), new Class[0]);
            return method.invoke(object, new Object[0]);
        } catch (Exception e) {
            return null;
        }
    }

}
