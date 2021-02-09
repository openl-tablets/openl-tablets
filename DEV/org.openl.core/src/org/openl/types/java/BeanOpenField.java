package org.openl.types.java;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.runtime.ContextProperty;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.ArrayTool;
import org.openl.util.ClassUtils;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author snshor
 *
 */
public final class BeanOpenField implements IOpenField {
    private static final Logger LOG = LoggerFactory.getLogger(BeanOpenField.class);

    private final PropertyDescriptor descriptor;
    private final Method readMethod;
    private final Method writeMethod;
    private final String contextProperty;

    public static void collectFields(Map<String, IOpenField> map, Class<?> c) {
        try {
            BeanInfo info = Introspector.getBeanInfo(c, c.getSuperclass());
            PropertyDescriptor[] pds = info.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                if (pd.getPropertyType() == null || "class".equals(pd.getName())) {
                    // (int) only method(s)
                    continue;
                }

                String fieldName = pd.getName();
                try {
                    c.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ex) {
                    // Catch it
                    // if there is no such field => it was
                    // named with the first letter as upper case
                    //
                    try {
                        String fname = ClassUtils.capitalize(fieldName);
                        Field field = c.getDeclaredField(fname);
                        // Reset the name
                        fieldName = field.getName();
                        pd.setName(fieldName);
                        LOG.debug("Error occurred: ", ex);
                    } catch (NoSuchFieldException e1) {
                        try {
                            // Special case for backward compatibility
                            // when getAB() was generated for 'aB' field name.
                            // In this case Introspector returns 'AB' field name.
                            String fname = StringUtils.uncapitalize(fieldName);
                            Field field = c.getDeclaredField(fname);
                            // Reset the name
                            fieldName = field.getName();
                            pd.setName(fieldName);
                            LOG.debug("Error occurred: ", e1);
                        } catch (NoSuchFieldException e) {
                            LOG.debug("Ignored error: ", e);
                            // It is possible that there is no such field at all
                        }
                    }

                }

                BeanOpenField bf = new BeanOpenField(pd);

                if (map.get(fieldName) == null || map.get(fieldName).isStatic()) {
                    map.put(fieldName, bf);
                }
            }
        } catch (Exception | LinkageError t) {
            throw RuntimeExceptionWrapper.wrap(t);
        }
    }

    private BeanOpenField(PropertyDescriptor descriptor) {
        this.descriptor = descriptor;
        this.readMethod = descriptor.getReadMethod();
        this.writeMethod = descriptor.getWriteMethod();
        ContextProperty contextPropertyAnnotation = null;
        if (this.readMethod != null) {
            contextPropertyAnnotation = this.readMethod.getAnnotation(ContextProperty.class);
        }
        this.contextProperty = contextPropertyAnnotation != null ? contextPropertyAnnotation.value() : null;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        try {
            if (target == null) {
                // assuming it is a non static read method.
                return getType().nullObject();
            }
            return readMethod.invoke(target, ArrayTool.ZERO_OBJECT);
        } catch (Exception ex) {
            throw RuntimeExceptionWrapper.wrap("", ex);
        }
    }

    @Override
    public IOpenClass getDeclaringClass() {
        if (descriptor.getReadMethod() != null) {
            return JavaOpenClass.getOpenClass(readMethod.getDeclaringClass());
        }
        if (descriptor.getWriteMethod() != null) {
            return JavaOpenClass.getOpenClass(writeMethod.getDeclaringClass());
        }
        throw new RuntimeException("Something is wrong with this bean");
    }

    @Override
    public String getDisplayName(int mode) {
        return getName();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public String getName() {
        return descriptor.getName();
    }

    @Override
    public IOpenClass getType() {
        return JavaOpenClass.getOpenClass(descriptor.getPropertyType());
    }

    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public boolean isReadable() {
        return readMethod != null;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isWritable() {
        return writeMethod != null;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        if (target != null) {
            try {
                writeMethod.invoke(target, value);
            } catch (Exception ex) {
                throw RuntimeExceptionWrapper.wrap("", ex);
            }
        }
    }

    @Override
    public String toString() {
        return getName();
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
