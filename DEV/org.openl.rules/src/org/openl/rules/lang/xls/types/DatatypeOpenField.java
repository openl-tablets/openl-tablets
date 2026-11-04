package org.openl.rules.lang.xls.types;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.exception.OpenLRuntimeException;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.types.java.MethodInvoker;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Open field for datatypes. Work with generated simple beans.
 *
 * @author Yury Molchan
 */
public class DatatypeOpenField extends AOpenField {

    private final AtomicReference<Accessors> accessors = new AtomicReference<>();

    private static final Object[] NO_ARGS = {};

    /** The invokers of the bean methods behind the field; either may be absent. */
    private record Accessors(@Nullable MethodInvoker reader, @Nullable MethodInvoker writer) {
        Accessors(@Nullable Method getter, @Nullable Method setter) {
            this(invoker(getter), invoker(setter));
        }

        private static @Nullable MethodInvoker invoker(@Nullable Method method) {
            return Optional.ofNullable(method).map(MethodInvoker::new).orElse(null);
        }

        private static @Nullable Method method(@Nullable MethodInvoker invoker) {
            return Optional.ofNullable(invoker).map(MethodInvoker::getMethod).orElse(null);
        }
    }

    @Getter
    private final boolean isTransient;
    @Getter
    private final IOpenClass declaringClass;
    @Getter
    private final String contextProperty;

    public DatatypeOpenField(IOpenClass declaringClass,
                             String name,
                             IOpenClass type,
                             String contextProperty,
                             boolean isTransient) {
        super(name, type);
        this.declaringClass = declaringClass;
        this.contextProperty = contextProperty;
        this.isTransient = isTransient;
    }

    @Override
    public boolean isContextProperty() {
        return contextProperty != null;
    }

    private Accessors accessors() {
        var methods = accessors.get();
        if (methods == null) {
            // Found on first use: the bean class that declares the accessors is generated after the field.
            synchronized (this) {
                methods = accessors.get();
                if (methods == null) {
                    methods = findAccessors();
                    accessors.set(methods);
                }
            }
        }
        return methods;
    }

    private Accessors findAccessors() {
        Class<?> instanceClass = declaringClass.getInstanceClass();
        String name = ClassUtils.capitalize(getName()); // According to JavaBeans v1.01
        Method getter = null;
        try {
            getter = instanceClass.getMethod("get" + name);
        } catch (NoSuchMethodException e) {
            name = StringUtils.capitalize(getName()); // Always capitalize (old behavior (prior 5.21.7)
            try {
                getter = instanceClass.getMethod("get" + name);
            } catch (NoSuchMethodException ignored) {
                // the class has no getter for the field under either name
            }
        }
        Method setter = null;
        try {
            // Use the same name as for the getter
            Class<?> type = getType().getInstanceClass();
            setter = instanceClass.getMethod("set" + name, type);
        } catch (NoSuchMethodException ignored) {
            // the class has no setter for the field
        }
        return new Accessors(getter, setter);
    }

    public @Nullable Method getGetter() {
        return Accessors.method(accessors().reader());
    }

    public @Nullable Method getSetter() {
        return Accessors.method(accessors().writer());
    }

    @Override
    public @Nullable Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return null;
        }
        try {
            var res = accessors().reader().invoke(target, NO_ARGS);
            return res != null ? res : getType().nullObject();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new OpenLRuntimeException(e);
        }
    }

    @Override
    public boolean isWritable() {
        // A generated datatype bean has a setter for every field.
        return true;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        var writer = accessors().writer();
        if (target != null) {
            try {
                writer.invoke(target, new Object[]{value});
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new OpenLRuntimeException(e);
            }
        }
    }
}
