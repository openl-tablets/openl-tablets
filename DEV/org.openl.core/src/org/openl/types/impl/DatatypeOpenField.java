package org.openl.types.impl;

import org.openl.message.OpenLMessagesUtils;
import org.openl.types.IOpenClass;
import org.openl.util.StringTool;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Open field for datatypes. Work with generated simple beans.
 *
 * @author DLiauchuk
 */
public class DatatypeOpenField extends AOpenField {

    private final Logger log = LoggerFactory.getLogger(DatatypeOpenField.class);

    private IOpenClass declaringClass;

    public DatatypeOpenField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(name, type);
        this.declaringClass = declaringClass;
    }

    public DatatypeOpenField(String name, IOpenClass type) {
        super(name, type);
    }

    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return null;
        }

        Object res = null;
        Class<?> targetClass = target.getClass();
        try {
            Method method;
            try {
                method = targetClass.getMethod(StringTool.getGetterName(getName()));
                res = method.invoke(target);
            } catch (NoSuchMethodException e1) {
                processError(e1);
            } catch (IllegalArgumentException e) {
                processError(e);
            } catch (IllegalAccessException e) {
                processError(e);
            } catch (InvocationTargetException e) {
                processError(e);
            }
        } catch (SecurityException e) {
            processError(e);
        }
        return res != null ? res : getType().nullObject();
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
        Class<?> targetClass = target.getClass();
        try {
            Method method;
            try {
                method = targetClass.getMethod(StringTool.getSetterName(getName()), getType().getInstanceClass());
                method.invoke(target, value);
            } catch (NoSuchMethodException e1) {
                String errorMessage = String.format("There is no setter method in class %s for the field %s with type %s",
                        targetClass.getSimpleName(), getName(), getType().getInstanceClass().getSimpleName());
                processError(errorMessage, e1);
            } catch (IllegalArgumentException e1) {
                // TODO: add business friendly message if needed
                processError(e1);
            } catch (IllegalAccessException e1) {
                // TODO: add business friendly message if needed
                processError(e1);
            } catch (InvocationTargetException e1) {
                // TODO: add business friendly message if needed
                processError(e1);
            }
        } catch (SecurityException e1) {
            processError(e1);
        }
    }

    private void processError(Throwable e1) {
        log.error("{}", this, e1);
        OpenLMessagesUtils.addError(e1);
    }

    private void processError(String errorMessage, Throwable e1) {
        log.error(errorMessage + "\n{}", this, e1);

        // add business friendly error description
        OpenLMessagesUtils.addError(errorMessage);
    }

    public void setDeclaringClass(IOpenClass declaringClass) {
        this.declaringClass = declaringClass;
    }

}
