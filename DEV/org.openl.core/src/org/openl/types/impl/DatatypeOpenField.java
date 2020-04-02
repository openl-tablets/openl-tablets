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
    private volatile Method getter;
    private volatile Method setter;
    private final String contextProperty;

    public DatatypeOpenField(IOpenClass declaringClass, String name, IOpenClass type, String contextProperty) {
        super(name, type);
        this.declaringClass = declaringClass;
        this.contextProperty = contextProperty;
    }

    private void initMethods() {
        if (flag == 0) {
            // TODO: Refactoring. Move this method to DatatypeTableBoundNode.processRow()
            // No needs in lazy-initialization in run-time when it is known in compile-time
            synchronized (this) {
                if (flag == 0) {
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
                    flag = 1;
                }
            }
        }

    }

    public Method getGetter() {
        initMethods();
        return getter;
    }

    public Method getSetter() {
        initMethods();
        return setter;
    }

    @Override
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

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        initMethods();
        if (target != null) {
            try {
                setter.invoke(target, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isContextProperty() {
        return contextProperty != null;
    }

    @Override
    public String getContextProperty() {
        return contextProperty;
    }
}
