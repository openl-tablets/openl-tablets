package org.openl.types.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.exception.OpenLRuntimeException;
import org.openl.types.IOpenClass;
import org.openl.util.StringTool;
import org.openl.vm.IRuntimeEnv;

/**
 * Open field for datatypes. Work with generated simple beans.
 * 
 * @author DLiauchuk
 *
 */
public class DatatypeOpenField extends AOpenField {
    
    private final Log LOG = LogFactory.getLog(DatatypeOpenField.class);

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
            throw new OpenLRuntimeException(String
                    .format("Can not get [%s] field from \"null\" object", this.getName()));
        }
        Object res = null;
        Class<?> targetClass = target.getClass();
        try {
            Method method;
            try {
                method = targetClass.getMethod(StringTool.getGetterName(getName()), new Class<?>[0]);
                res = method.invoke(target, new Object[0]);
            } catch (NoSuchMethodException e1) {
                LOG.error(this, e1);
            } catch (IllegalArgumentException e) {
                LOG.error(this, e);
            } catch (IllegalAccessException e) {
                LOG.error(this, e);
            } catch (InvocationTargetException e) {
                LOG.error(this, e);
            }
        } catch (SecurityException e) {
            LOG.error(this, e);
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
                LOG.error(this, e1);
            } catch (IllegalArgumentException e) {
                LOG.error(this, e);
            } catch (IllegalAccessException e) {
                LOG.error(this, e);
            } catch (InvocationTargetException e) {
                LOG.error(this, e);
            }
        } catch (SecurityException e) {
            LOG.error(this, e);
        }
    }

    public void setDeclaringClass(IOpenClass declaringClass) {
        this.declaringClass = declaringClass;
    }

}
