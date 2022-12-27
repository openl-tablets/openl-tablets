package org.openl.rules.testmethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.AOpenField;
import org.openl.types.impl.ThisField;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;

public class UserErrorOpenClass extends ADynamicClass {

    public UserErrorOpenClass() {
        super("UserError", Object.class);
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        return new Entry();
    }

    public IOpenField getField(String name, boolean strictMatch) {
        IOpenField field = super.getField(name, strictMatch);
        if (field != null) {
            return field;
        }
        field = new DynamicField(this, name);
        addField(field);
        return field;
    }

    @Override
    public IOpenClass getComponentClass() {
        return new UserErrorOpenClass();
    }

    public static class Entry {
        final Object value;

        private Entry() {
            this.value = new HashMap<>(4);
        }

        public Entry(String value) {
            this.value = value;
        }

        public Entry(Object value) {
            this.value = value;
        }

        public Object get() {
            return value;
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }
    }

    private static class DynamicField extends AOpenField {

        private final IOpenClass declaringClass;

        public DynamicField(IOpenClass declaringClass, String name) {
            super(name, new UserErrorOpenClass());
            this.declaringClass = declaringClass;
        }

        @Override
        public Object get(Object target, IRuntimeEnv env) {
            if (target == null) {
                return null;
            } else if (target instanceof Entry) {
                Object o = ((Map<?, ?>) ((Entry) target).value).get(getName());
                if (o instanceof Entry && ((Entry) o).value instanceof String) {
                    return ((Entry) o).value;
                }
                return o;
            } else if (ThisField.THIS.equals(getName())) {
                return target;
            } else {
                var targetClass = target.getClass();
                try {
                    var field = targetClass.getField(getName());
                    if (Modifier.isPublic(field.getModifiers())) {
                        return field.get(target);
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                }
                var name = ClassUtils.capitalize(getName()); // According to JavaBeans v1.01
                try {
                    var getter = targetClass.getMethod("get" + name);
                    return getter.invoke(target);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                }
                return null;
            }
        }

        @Override
        public IOpenClass getDeclaringClass() {
            return declaringClass;
        }

        @Override
        public boolean isWritable() {
            return true;
        }

        @Override
        public void set(Object target, Object value, IRuntimeEnv env) {
            ((Map) ((Entry) target).value).put(getName(), value);
        }
    }
}
