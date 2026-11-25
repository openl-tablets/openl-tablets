package org.openl.types.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.classloader.OpenLClassLoader;
import org.openl.gen.InterfaceImplBuilder;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodKey;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.util.ClassUtils;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.IRuntimeEnv;

/**
 * A Java interface as an OpenL type.
 *
 * <p>A new instance of a collection or a map is an object of the JDK that implements the interface, such as an
 * {@link ArrayList} for a list, a {@link TreeSet} for a sorted set or an {@link ArrayDeque} for a queue. A new
 * instance of any other public interface is a bean that
 * {@link InterfaceImplBuilder} generates, one class for each interface. The bean keeps a value for every property of
 * the interface and of the interfaces it extends, and every getter and setter of them reads or writes that value.
 *
 * <p>Rules only read a property that has no setter. A Data or a Test table sets it on the bean it builds, through the
 * field {@link #getFieldToFill(IOpenField)} returns.
 */
// A Java type is equal by its Java class; the state added here is derived from that class.
@SuppressWarnings("java:S2160")
@NullMarked
@Slf4j
public class JavaOpenInterface extends JavaOpenClass {

    /**
     * The bean of each interface of the engine, of a project or of a library it brings, kept as long as the interface
     * lives, whatever OpenL type stands for it.
     */
    private static final ClassValue<Class<?>> BEANS = new ClassValue<>() {
        @Override
        protected Class<?> computeValue(Class<?> type) {
            return generateBean(type);
        }
    };

    /**
     * The bean of each interface that may stay loaded after the engine, such as one of the JDK or of a library of the
     * server, kept as long as the engine runs. Kept with such an interface, the bean would keep the engine in memory
     * after it is undeployed.
     */
    private static final Map<Class<?>, Class<?>> ENGINE_BEANS = new ConcurrentHashMap<>();

    /** The setter of each property of a bean, kept as long as the bean lives, or none for a property it keeps not. */
    private static final ClassValue<Map<String, Optional<MethodInvoker>>> SETTERS = new ClassValue<>() {
        @Override
        protected Map<String, Optional<MethodInvoker>> computeValue(Class<?> bean) {
            return new ConcurrentHashMap<>();
        }
    };

    /** The collections and the maps a new instance of a collection or a map interface is, the preferred first. */
    private static final Map<Class<?>, Supplier<Object>> COLLECTIONS = collections();

    /** How the objects of the interface are made, worked out once. */
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final Construction construction = construct();

    /**
     * How the objects of an interface are made.
     *
     * @param bean    the bean of the interface, or {@code null} when OpenL does not implement the interface
     * @param factory creates a new object, or throws why it cannot
     */
    private record Construction(@Nullable Class<?> bean, Supplier<Object> factory) {
    }

    JavaOpenInterface(Class<?> instanceClass) {
        super(instanceClass);
    }

    @Override
    protected Map<MethodKey, IOpenMethod> initMethodMap() {
        var methodMap = new HashMap<MethodKey, IOpenMethod>(super.initMethodMap());

        // Any interface has the methods of Object, such as toString().
        for (IOpenMethod om : JavaOpenClass.OBJECT.getMethods()) {
            methodMap.put(new MethodKey(om), om);
        }

        return Collections.unmodifiableMap(methodMap);
    }

    /**
     * Creates an object of the interface: a collection, a map or the bean of the interface.
     *
     * @throws IllegalStateException when OpenL cannot implement the interface: it is sealed, not public, in a
     *                               package its module does not export, or a collection the JDK does not implement
     */
    @Override
    public Object newInstance(IRuntimeEnv env) {
        return getConstruction().factory().get();
    }

    /**
     * Returns the field a Data or a Test table fills a property of this interface with.
     *
     * <p>A property without a setter is set on the bean that the table builds. The field reads the property through
     * the interface, so it reads it from any object of the interface, and writes it into the bean. Its type is the
     * type the bean keeps: the narrowest one when the interfaces declare the property with several types.
     *
     * <p>Any other field is returned as it is, and the table reports it as not writable. So is a property the bean
     * does not keep, such as one a default getter computes, and a field of a collection, of a map and of an interface
     * that OpenL cannot implement: sealed, not public, in a package its module does not export, or failing to build.
     */
    public IOpenField getFieldToFill(IOpenField field) {
        var bean = getConstruction().bean();
        if (bean == null || field.isWritable() || !field.isReadable() || field.isStatic()) {
            return field;
        }
        return SETTERS.get(bean)
                .computeIfAbsent(field.getName(), property -> setterOf(bean, property))
                .<IOpenField>map(setter -> new FilledProperty(field, setter))
                .orElse(field);
    }

    private static Optional<MethodInvoker> setterOf(Class<?> bean, String property) {
        try {
            var type = bean.getDeclaredField(property).getType();
            return Optional.of(new MethodInvoker(bean.getMethod(ClassUtils.setter(property), type)));
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    private Construction construct() {
        var collection = collectionFactory();
        if (collection != null) {
            return new Construction(null, collection);
        }
        if (!isBuiltAsBean()) {
            var message = """
                    Cannot create an object of the interface '%s': it is sealed, not public, in a package its module\
                     does not export, or a collection the JDK does not implement.""".formatted(getName());
            return refused(() -> new IllegalStateException(message));
        }
        try {
            var type = getInstanceClass();
            var bean = mayOutliveEngine(type) ? ENGINE_BEANS.computeIfAbsent(type, JavaOpenInterface::generateBean)
                    : BEANS.get(type);
            var constructor = bean.getDeclaredConstructor();
            return new Construction(bean, () -> newBean(constructor));
        } catch (NoSuchMethodException | RuntimeException | LinkageError e) {
            log.debug("Failed to build a bean of the interface '{}'.", getName(), e);
            return refused(() -> RuntimeExceptionWrapper.wrap(e));
        }
    }

    /** The construction of an interface OpenL does not implement: every new object throws why. */
    private static Construction refused(Supplier<RuntimeException> error) {
        return new Construction(null, () -> {
            throw error.get();
        });
    }

    private static Object newBean(Constructor<?> constructor) {
        try {
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    private boolean isBuiltAsBean() {
        var instanceClass = getInstanceClass();
        return Modifier.isPublic(instanceClass.getModifiers())
                && !instanceClass.isSealed()
                && instanceClass.getModule().isExported(instanceClass.getPackageName())
                && !isCollectionOrMap(instanceClass);
    }

    private static boolean isCollectionOrMap(Class<?> type) {
        return Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type);
    }

    /**
     * Tells whether the interface may stay loaded after the engine is unloaded.
     *
     * <p>It may not when its class loader or a parent of it is the class loader of the engine or one OpenL creates:
     * either keeps the engine loaded. Any other interface may, such as one of the JDK, of a library of the server or of
     * another application.
     */
    static boolean mayOutliveEngine(Class<?> type) {
        var engineLoader = JavaOpenInterface.class.getClassLoader();
        return findLoader(type.getClassLoader(), loader -> loader == engineLoader || loader instanceof OpenLClassLoader)
                .isEmpty();
    }

    /**
     * Generates the bean in the nearest OpenL class loader of the interface, that of its project, so that the project
     * finds the bean by its name.
     *
     * <p>The bean of an interface of the engine, of the JDK or of a library of the server stays under the class loader
     * of the interface, so it lives as long as the interface does.
     */
    private static Class<?> generateBean(Class<?> type) {
        var builder = new InterfaceImplBuilder(type);
        try {
            return ClassLoaderUtils.defineClass(builder.getBeanName(), builder.byteCode(), beanClassLoader(type));
        } catch (ClassNotFoundException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    private static ClassLoader beanClassLoader(Class<?> type) {
        var interfaceLoader = Objects.requireNonNullElseGet(type.getClassLoader(),
                ClassLoader::getPlatformClassLoader);
        return findLoader(interfaceLoader, loader -> loader instanceof OpenLClassLoader && isLoadedBy(type, loader))
                .orElse(interfaceLoader);
    }

    /** Returns the first of the class loader and its parents that matches. */
    private static Optional<ClassLoader> findLoader(@Nullable ClassLoader loader, Predicate<ClassLoader> matches) {
        return Stream.iterate(loader, Objects::nonNull, ClassLoader::getParent)
                .filter(matches)
                .findFirst();
    }

    private static boolean isLoadedBy(Class<?> type, ClassLoader loader) {
        try {
            return Class.forName(type.getName(), false, loader) == type;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private @Nullable Supplier<Object> collectionFactory() {
        var instanceClass = getInstanceClass();
        if (!isCollectionOrMap(instanceClass)) {
            return null;
        }
        return COLLECTIONS.entrySet()
                .stream()
                .filter(collection -> instanceClass.isAssignableFrom(collection.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    private static Map<Class<?>, Supplier<Object>> collections() {
        var collections = new LinkedHashMap<Class<?>, Supplier<Object>>();
        collections.put(ArrayList.class, ArrayList::new);
        collections.put(HashSet.class, HashSet::new);
        collections.put(LinkedHashSet.class, LinkedHashSet::new);
        collections.put(TreeSet.class, TreeSet::new);
        collections.put(ArrayDeque.class, ArrayDeque::new);
        collections.put(HashMap.class, HashMap::new);
        collections.put(LinkedHashMap.class, LinkedHashMap::new);
        collections.put(TreeMap.class, TreeMap::new);
        collections.put(ConcurrentHashMap.class, ConcurrentHashMap::new);
        collections.put(ConcurrentSkipListMap.class, ConcurrentSkipListMap::new);
        return Collections.unmodifiableMap(collections);
    }

    @Override
    public boolean isInterface() {
        return true;
    }

    /**
     * A property of an interface that a table fills: it reads the value through the interface and writes it into the
     * bean with the setter of the bean, as a value of the type the bean keeps.
     */
    private static final class FilledProperty extends OpenFieldDelegator {

        private final MethodInvoker setter;
        @Getter
        private final IOpenClass type;

        private FilledProperty(IOpenField reader, MethodInvoker setter) {
            super(reader);
            this.setter = setter;
            this.type = JavaOpenClass.getOpenClass(setter.getMethod().getParameterTypes()[0]);
        }

        @Override
        public void set(@Nullable Object target, @Nullable Object value, IRuntimeEnv env) {
            if (target == null) {
                // Like the field of a bean, it sets nothing on no object
                return;
            }
            if (!setter.getMethod().getDeclaringClass().isInstance(target)) {
                throw new IllegalStateException("Cannot set the property '%s' of '%s', which has no setter of it."
                        .formatted(getName(), target.getClass().getTypeName()));
            }
            try {
                setter.invoke(target, new Object[]{value});
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }

        @Override
        public boolean isWritable() {
            return true;
        }

        @Override
        public boolean isContextProperty() {
            return delegate.isContextProperty();
        }

        @Override
        public @Nullable String getContextProperty() {
            return delegate.getContextProperty();
        }

        @Override
        public boolean isTransient() {
            return delegate.isTransient();
        }

        // Unlike the delegator, it equals no field but itself.
        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }
}
