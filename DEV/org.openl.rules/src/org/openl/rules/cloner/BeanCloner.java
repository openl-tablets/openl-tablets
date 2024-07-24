package org.openl.rules.cloner;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * This cloner use Java Bean introspection to get public properties and fields, and using them to copy data to the
 * new created Java Bean instance.
 *
 * @author Yury Molchan
 */
class BeanCloner<T> implements ICloner<T> {

    private final Map<String, GetSetter> fields = new HashMap<>();

    BeanCloner(Class<T> clazz) {
        var allFields = clazz.getFields();
        for (var field : allFields) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
                this.fields.put(field.getName(), new FieldGetSetter(field));
            }
        }

        try {
            var beanInfo = Introspector.getBeanInfo(clazz);
            var props = beanInfo.getPropertyDescriptors();
            for (var prop : props) {
                if (fields.containsKey(prop.getName())) {
                    // Ignore, let's use public field for cloning
                    continue;
                } else if (prop.getReadMethod() != null && prop.getWriteMethod() != null) {
                    this.fields.put(prop.getName(), new PropertyGetSetter(prop));
                } else if ("class".equals(prop.getName())) {
                    // The special case when a 'class' property is defined in the OpenL Datatype
                    try {
                        var getter = clazz.getMethod("getClass");
                        var returnType = getter.getReturnType();
                        if (!Class.class.equals(returnType)) {
                            var setter = clazz.getMethod("setClass", returnType);
                            prop.setReadMethod(getter);
                            prop.setWriteMethod(setter);
                            this.fields.put(prop.getName(), new PropertyGetSetter(prop));
                        }
                    } catch (NoSuchMethodException ignore) {
                        continue;
                    }
                } else if (prop.getReadMethod() != null || prop.getWriteMethod() != null) {
                    // WARN
                    continue;
                }
            }
        } catch (IntrospectionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object getInstance(Object source) {
        try {
            return source.getClass().getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new IllegalArgumentException(source.getClass().getName() + " bean is non-public or haven't the default constructor.", e);
        }
    }

    @Override
    public void clone(T source, Function<Object, Object> cloner, T target) {
        for (var field : fields.values()) {
            try {
                var fieldObject = field.get(source);
                var res = cloner.apply(fieldObject);
                field.set(target, res);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    interface GetSetter {
        void set(Object target, Object value) throws InvocationTargetException, IllegalAccessException;

        Object get(Object source) throws InvocationTargetException, IllegalAccessException;
    }

    static class FieldGetSetter implements GetSetter {

        private final Field field;

        public FieldGetSetter(Field field) {
            this.field = field;
        }

        @Override
        public void set(Object target, Object value) throws IllegalAccessException {
            field.set(target, value);
        }

        @Override
        public Object get(Object source) throws IllegalAccessException {
            return field.get(source);
        }
    }

    static class PropertyGetSetter implements GetSetter {
        private final PropertyDescriptor field;

        public PropertyGetSetter(PropertyDescriptor field) {
            this.field = field;
        }

        @Override
        public void set(Object target, Object value) throws InvocationTargetException, IllegalAccessException {
            field.getWriteMethod().invoke(target, value);
        }

        @Override
        public Object get(Object source) throws InvocationTargetException, IllegalAccessException {
            return field.getReadMethod().invoke(source);
        }
    }
}
