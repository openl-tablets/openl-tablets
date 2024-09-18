package org.openl.rules.cloner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.openl.util.ClassUtils;

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

        var methods = clazz.getMethods();
        var setters = new HashMap<String, Method>();
        var getters = new HashMap<String, Method>();
        for (var method : methods) {
            if (Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            var methodName = method.getName();
            if (methodName.startsWith("get") && method.getParameterCount() == 0) {
                getters.put(ClassUtils.toFieldName(methodName), method);
            } else if (methodName.startsWith("is") && method.getParameterCount() == 0 && method.getReturnType().equals(Boolean.TYPE)) {
                getters.put(ClassUtils.decapitalize(methodName.substring(2)), method);
            } else if (methodName.startsWith("set") && method.getParameterCount() == 1 && method.getReturnType().equals(Void.TYPE)) {
                setters.put(ClassUtils.toFieldName(methodName), method);
            }
        }
        for (var v : setters.entrySet()) {
            var fieldName = v.getKey();
            if (fields.containsKey(fieldName)) {
                // Ignore, let's use public field for cloning
                continue;
            }
            var setter = v.getValue();
            var getter = getters.get(fieldName);
            if (getter != null) {
                this.fields.put(fieldName, new PropertyGetSetter(getter, setter));
            }
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
        private final Method getter;
        private final Method setter;

        public PropertyGetSetter(Method getter, Method setter) {
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public void set(Object target, Object value) throws InvocationTargetException, IllegalAccessException {
            setter.invoke(target, value);
        }

        @Override
        public Object get(Object source) throws InvocationTargetException, IllegalAccessException {
            return getter.invoke(source);
        }
    }
}
