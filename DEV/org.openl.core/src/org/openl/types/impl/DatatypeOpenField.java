package org.openl.types.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Open field for datatypes. Work with generated simple beans.
 *
 * @author Yury Molchan
 */
public class DatatypeOpenField extends AOpenField {

    private IOpenClass declaringClass;
    private volatile byte flag;
    private Method getter;
    private Method setter;

    public DatatypeOpenField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(name, type);
        this.declaringClass = declaringClass;
    }

    private void initMethods() {
        if (flag == 0) {
            synchronized (this) {
                if (flag == 0) {
                    flag = 1;
                    Class<?> instanceClass = declaringClass.getInstanceClass();
                    String name = ClassUtils.capitalize(getName()); // According to JavaBeans v1.01
                    try {
                        getter = instanceClass.getMethod("get" + name);
                    } catch (NoSuchMethodException e) {
                        name = StringUtils.capitalize(getName()); // Always capitalize (old behavior (prior 5.21.7)
                        try {
                            getter = instanceClass.getMethod("get" + name);
                        } catch (NoSuchMethodException e1) {
                            throw new RuntimeException(e);
                        }
                    }
                    try {
                        // Use the same name as for the getter
                        Class<?> type = getType().getInstanceClass();
                        setter = instanceClass.getMethod("set" + name, type);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

    }

    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return null;
        }

        initMethods();
        try {
            Object res = getter.invoke(target);
            return res != null ? res : getType().nullObject();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public boolean isWritable() {
        // TODO check final attribute
        return true;
    }

    public void set(Object target, Object value, IRuntimeEnv env) {
        initMethods();
        try {
            setter.invoke(target, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
