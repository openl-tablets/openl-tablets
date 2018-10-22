package org.openl.types.java;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.ArrayTool;
import org.openl.util.ClassUtils;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class BeanOpenField implements IOpenField {

    PropertyDescriptor descriptor;
    Method readMethod;
    Method writeMethod;

    static public void collectFields(Map<String, IOpenField> map, Class<?> c, Map<Method, BeanOpenField> getters,
            Map<Method, BeanOpenField> setters) {

        if (c.isInterface()) {
            Class<?>[] interfaces = c.getInterfaces();
            for (Class<?> anInterface : interfaces) {
                collectFields(map, anInterface, getters, setters);
            }
        }

        try {
            BeanInfo info = Introspector.getBeanInfo(c);
            PropertyDescriptor[] pds = info.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                if (pd.getPropertyType() == null) {
                    // (int) only method(s)
                    continue;
                }
                if (pd.getName().equals("class")) {
                    continue;
                }

                String fieldName = pd.getName();
                Field field = null;
                try {
                    field = c.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ex) {
                    // Catch it
                    // if there is no such field => it was
                    // named with the first letter as upper case
                    //
                    try {
                        String fname = ClassUtils.capitalize(fieldName);
                        field = c.getDeclaredField(fname);
                        // Reset the name
                        fieldName = field.getName();
                        pd.setName(fieldName);
                    } catch (NoSuchFieldException e1) {
                        try {
                            // Special case for backward compatibility
                            // when getAB() was generated for 'aB' field name.
                            // In this case Introspector returns 'AB' field name.
                            String fname = StringUtils.uncapitalize(fieldName);
                            field = c.getDeclaredField(fname);
                            // Reset the name
                            fieldName = field.getName();
                            pd.setName(fieldName);
                        } catch (NoSuchFieldException e2) {
                            // It is possible that there is no such field at all
                            //
                        }
                    }

                }
                BeanOpenField bf = new BeanOpenField(pd);
                
                if (field == null || !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                	map.put(fieldName, bf);
                	if (getters != null) {
                        if (pd.getReadMethod() != null) {
                            getters.put(pd.getReadMethod(), bf);
                        }
                    }
                    if (setters != null) {
                        if (pd.getWriteMethod() != null) {
                            setters.put(pd.getWriteMethod(), bf);
                        }
                    }
                } 
            }
        } catch (Throwable t) {
            throw RuntimeExceptionWrapper.wrap(t);
        }

    }

    /**
     *
     */
    private BeanOpenField(PropertyDescriptor descriptor) {
        this.descriptor = descriptor;
        this.readMethod = descriptor.getReadMethod();
        this.writeMethod = descriptor.getWriteMethod();
    }

    /**
     *
     */

    public Object get(Object target, IRuntimeEnv env) {
        try {
            if (target == null) {
                // assuming it is a non static read method.
                return null;
            }
            return readMethod.invoke(target, ArrayTool.ZERO_OBJECT);
        } catch (Exception ex) {
            throw RuntimeExceptionWrapper.wrap("", ex);
        }
    }

    /**
     *
     */

    public IOpenClass getDeclaringClass() {
        if (descriptor.getReadMethod() != null) {
            return JavaOpenClass.getOpenClass(readMethod.getDeclaringClass());
        }
        if (descriptor.getWriteMethod() != null) {
            return JavaOpenClass.getOpenClass(writeMethod.getDeclaringClass());
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
        return readMethod != null;
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
        return writeMethod != null;
    }

    /**
     *
     */

    public void set(Object target, Object value, IRuntimeEnv env) {
        try {
            writeMethod.invoke(target, value);
        } catch (Exception ex) {
            throw RuntimeExceptionWrapper.wrap("", ex);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

}
