package org.openl.rules.testmethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.calc.SpreadsheetResult;
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

    @Override
    public IOpenField getField(String name, boolean strictMatch) {
        var field = super.getField(name, strictMatch);
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

    @RequiredArgsConstructor
    public static class Entry {
        final Object value;

        private Entry() {
            this.value = HashMap.newHashMap(4);
        }

        public Entry(String value) {
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

        @Getter
        private final IOpenClass declaringClass;

        public DynamicField(IOpenClass declaringClass, String name) {
            super(name, new UserErrorOpenClass());
            this.declaringClass = declaringClass;
        }

        @Override
        public Object get(Object target, IRuntimeEnv env) {
            if (target == null) {
                return null;
            } else if (target instanceof Entry entry1) {
                var o = ((Map<?, ?>) entry1.value).get(getName());
                if (o instanceof Entry entry && ((Entry) o).value instanceof String) {
                    return entry.value;
                }
                return o;
            } else if (ThisField.THIS.equals(getName())) {
                return target;
            } else if (target instanceof SpreadsheetResult result) {
                if (!result.hasField(getName())) {
                    throw notFound(target);
                }
                return result.getFieldValue(getName());
            } else {
                return readProperty(target);
            }
        }

        /**
         * Reads the value of a public field or of an accessor of the target: a JavaBeans getter, {@code getX} or
         * {@code isX}, or the accessor of a record component.
         *
         * @throws OpenLRuntimeException if the target has none of them
         */
        private @Nullable Object readProperty(Object target) {
            var targetClass = target.getClass();
            try {
                return targetClass.getField(getName()).get(target);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                // there is no public field to read, so an accessor is tried
            }
            var accessor = accessorOf(targetClass);
            if (accessor == null) {
                throw notFound(target);
            }
            try {
                return accessor.invoke(target);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                // the accessor exists but cannot give a value, so there is none
                return null;
            }
        }

        /** The accessor of the field in the given class, or {@code null} when it has none. */
        private @Nullable Method accessorOf(Class<?> targetClass) {
            var capitalized = ClassUtils.capitalize(getName()); // According to JavaBeans v1.01
            var names = targetClass.isRecord() ? List.of(getName()) : List.of("get" + capitalized, "is" + capitalized);
            for (var name : names) {
                try {
                    return targetClass.getMethod(name);
                } catch (NoSuchMethodException ignored) {
                    // the next form of the accessor is tried
                }
            }
            return null;
        }

        private OpenLRuntimeException notFound(Object target) {
            return new OpenLRuntimeException("Field '%s' is not found in the error of type '%s'."
                    .formatted(getName(), target.getClass().getSimpleName()));
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
