/*
 * Created on Oct 17, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openl.exception.OpenLRuntimeException;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.ArrayTool;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class BeanOpenField implements IOpenField {

    PropertyDescriptor descriptor;

    static public void collectFields(Map<String, IOpenField> map, Class<?> c, Map<Method, BeanOpenField> getters,
            Map<Method, BeanOpenField> setters) {

        if (c.isInterface()) {
            Class<?>[] interfaces = c.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                collectFields(map, interfaces[i], getters, setters);
            }
        }

        try {
            BeanInfo info = Introspector.getBeanInfo(c);
            PropertyDescriptor[] pd = info.getPropertyDescriptors();
            for (int i = 0; i < pd.length; i++) {
                if (pd[i].getPropertyType() == null) {
                    // (int) only method(s)
                    continue;
                }
                if (pd[i].getName().equals("class")) {
                    continue;
                }
                BeanOpenField bf = new BeanOpenField(pd[i]);
                map.put(pd[i].getName(), bf);
                if (getters != null) {
                    if (pd[i].getReadMethod() != null) {
                        getters.put(pd[i].getReadMethod(), bf);
                    }
                }
                if (setters != null) {
                    if (pd[i].getWriteMethod() != null) {
                        setters.put(pd[i].getWriteMethod(), bf);
                    }
                }
            }
        } catch (Throwable t) {
            throw new OpenLRuntimeException(t);
        }

    }

    /**
     *
     */
    public BeanOpenField(PropertyDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    /**
     *
     */

    public Object get(Object target, IRuntimeEnv env) {
        try {
            return descriptor.getReadMethod().invoke(target, ArrayTool.ZERO_OBJECT);
        } catch (Exception ex) {
            throw new OpenLRuntimeException(StringUtils.EMPTY, ex);
        }
    }

    /**
     *
     */

    public IOpenClass getDeclaringClass() {
        if (descriptor.getReadMethod() != null) {
            return JavaOpenClass.getOpenClass(descriptor.getReadMethod().getDeclaringClass());
        }
        if (descriptor.getWriteMethod() != null) {
            return JavaOpenClass.getOpenClass(descriptor.getWriteMethod().getDeclaringClass());
        }
        throw new RuntimeException("Something is wrong with this bean");
    }

    public String getDisplayName(int mode) {
        return getName();
    }

    /**
     *
     */

    public IMemberMetaInfo getInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     *
     */

    public String getName() {
        return descriptor.getName();
    }

    /**
     *
     */

    public IOpenClass getType() {
        return JavaOpenClass.getOpenClass(descriptor.getPropertyType());
    }

    /**
     *
     */

    public boolean isConst() {
        return false;
    }

    /**
     *
     */

    public boolean isReadable() {
        return descriptor.getReadMethod() != null;
    }

    /**
     *
     */

    public boolean isStatic() {
        return false;
    }

    /**
     *
     */

    public boolean isWritable() {
        return descriptor.getWriteMethod() != null;
    }

    /**
     *
     */

    public void set(Object target, Object value, IRuntimeEnv env) {
        try {
            descriptor.getWriteMethod().invoke(target, new Object[] { value });
        } catch (Exception ex) {
            throw new OpenLRuntimeException(StringUtils.EMPTY, ex);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

}
