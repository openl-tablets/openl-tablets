package org.openl.gen;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;

import org.openl.gen.groovy.GroovyInterfaceImplGenerator;
import org.openl.util.ClassUtils;

/**
 * Builds a bean that implements an interface.
 *
 * <p>The bean keeps a value for every property of the interface and of the interfaces it extends, in the narrowest
 * type they declare it with. Every getter and setter of the interfaces reads or writes that value, converting it to
 * the type of the method: a wider type, or a primitive for its wrapper. One of a type the value does not fit throws
 * {@link UnsupportedOperationException}.
 *
 * <p>The other methods do nothing and return the default value of their type. The default methods of the interfaces
 * and the methods of {@code Object}, which the bean writes from its values, stay as they are, but for a default getter
 * or setter of a property the interfaces declare an abstract accessor of: the bean keeps such a property and writes
 * both its getter and its setter.
 *
 * @author Vladyslav Pikus
 */
@NullMarked
public class InterfaceImplBuilder {

    private static final String DEFAULT_PACKAGE = "org.openl.generated";
    private static final String NAME_PATTERN = "%s.$%o%sImpl";
    private static final String SCRIPT_NAME_PATTERN = "%s.%s%oImpl";
    private static final AtomicInteger counter = new AtomicInteger();

    /** The public methods of {@code Object}, which the bean gets from it. */
    private static final Set<String> OBJECT_METHODS = Arrays.stream(Object.class.getMethods())
            .map(InterfaceImplBuilder::signatureOf)
            .collect(Collectors.toUnmodifiableSet());

    /** A getter or a setter of a property, as a method of an interface declares it: a getter takes no parameter. */
    private record PropertyMethod(String property, Class<?> type) {
    }

    private final Class<?> clazzInterface;
    private final String beanName;
    private final String scriptName;
    private final Map<String, Class<?>> propertyTypes = new TreeMap<>();
    private final List<MethodDescription> beanStubMethods = new ArrayList<>();
    private final List<JavaInterfaceImplGenerator.Accessor> accessors = new ArrayList<>();

    public InterfaceImplBuilder(Class<?> clazzInterface, String packagePath) {
        if (!clazzInterface.isInterface()) {
            throw new IllegalArgumentException("Target class is not an interface.");
        }
        this.clazzInterface = clazzInterface;
        this.beanName = NAME_PATTERN
                .formatted(packagePath, counter.incrementAndGet(), clazzInterface.getSimpleName());
        this.scriptName = SCRIPT_NAME_PATTERN
                .formatted(packagePath, clazzInterface.getSimpleName(), counter.incrementAndGet());
        init();
    }

    public InterfaceImplBuilder(Class<?> clazzInterface) {
        this(clazzInterface, DEFAULT_PACKAGE);
    }

    /**
     * Collects the methods the bean implements: every abstract method of the interface, each one in the narrowest
     * declaration of the interfaces. A method a nearer interface implements by default is left to it. The methods are
     * taken in the order of their names, so that an interface always gets the same bean.
     */
    private void init() {
        var methods = Arrays.stream(clazzInterface.getMethods())
                .filter(InterfaceImplBuilder::isImplemented)
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Method::getName)
                        .thenComparing(m -> Type.getMethodDescriptor(m)))));
        for (var method : methods) {
            var propertyMethod = propertyMethodOf(method);
            if (propertyMethod != null) {
                addPropertyType(propertyMethod.property(), propertyMethod.type());
            }
        }
        for (var method : methods) {
            addMethod(method);
        }
    }

    /** Tells whether the bean implements the method: the interface neither implements it nor gets it from Object. */
    private static boolean isImplemented(Method method) {
        return !method.isDefault() && !Modifier.isStatic(method.getModifiers())
                && !OBJECT_METHODS.contains(signatureOf(method));
    }

    /** The name and the parameter types of a method, which a method of an interface shares with one it overrides. */
    private static String signatureOf(Method method) {
        return method.getName() + Arrays.toString(method.getParameterTypes());
    }

    /**
     * Returns the property a method reads or writes, as {@link java.beans.Introspector} names it, or {@code null} for
     * any other method, such as a getter with a parameter.
     */
    private static @Nullable PropertyMethod propertyMethodOf(Method method) {
        var name = method.getName();
        var parameters = method.getParameterTypes();
        var returnType = method.getReturnType();
        if (parameters.length == 0 && returnType != void.class && name.length() > 3 && name.startsWith("get")) {
            return new PropertyMethod(ClassUtils.toFieldName(name), returnType);
        }
        if (parameters.length == 0 && returnType == boolean.class && name.length() > 2 && name.startsWith("is")) {
            return new PropertyMethod(ClassUtils.decapitalize(name.substring(2)), returnType);
        }
        if (parameters.length == 1 && returnType == void.class && name.length() > 3 && name.startsWith("set")) {
            return new PropertyMethod(ClassUtils.toFieldName(name), parameters[0]);
        }
        return null;
    }

    /**
     * Adds a type a property is declared with. The bean keeps the narrowest type of them, and the wrapper of a
     * primitive declared as both. Of two unrelated types, the one of the getter is kept.
     */
    private void addPropertyType(String property, Class<?> type) {
        var known = propertyTypes.get(property);
        if (known == null || known.isAssignableFrom(type) || ClassUtils.primitiveToWrapper(known) == type) {
            propertyTypes.put(property, type);
        }
    }

    /**
     * Adds a method the bean implements: an accessor of a property that reads or writes its value, unless it is the
     * getter or the setter the bean writes itself, or a stub.
     */
    private void addMethod(Method method) {
        var description = new MethodDescription(method.getName(),
                method.getReturnType(),
                method.getParameterTypes(),
                null);
        var propertyMethod = propertyMethodOf(method);
        if (propertyMethod == null) {
            beanStubMethods.add(description);
        } else if (!isBeanAccessor(method, propertyMethod)) {
            var property = propertyMethod.property();
            accessors.add(new JavaInterfaceImplGenerator.Accessor(description,
                    property,
                    fits(propertyMethod.type(), propertyTypes.get(property))));
        }
    }

    /** Tells whether the method is the getter or the setter the bean writes for the property. */
    private boolean isBeanAccessor(Method method, PropertyMethod propertyMethod) {
        var property = propertyMethod.property();
        var name = method.getParameterCount() == 0 ? ClassUtils.getter(property) : ClassUtils.setter(property);
        return method.getName().equals(name) && propertyMethod.type() == propertyTypes.get(property);
    }

    /**
     * Tells whether a value of one type converts to the other: a reference type to its subtype or supertype, a
     * primitive to its wrapper and back.
     */
    private static boolean fits(Class<?> methodType, Class<?> valueType) {
        return methodType.isAssignableFrom(valueType)
                || valueType.isAssignableFrom(methodType)
                || ClassUtils.primitiveToWrapper(methodType) == valueType
                || ClassUtils.primitiveToWrapper(valueType) == methodType;
    }

    public String getBeanName() {
        return beanName;
    }

    public String getScriptName() {
        return scriptName;
    }

    public byte[] byteCode() {
        var beanFields = new TreeMap<String, FieldDescription>();
        propertyTypes.forEach((property, type) -> beanFields.put(property, new FieldDescription(type.getName())));
        return new JavaInterfaceImplGenerator(beanName, clazzInterface, beanFields, beanStubMethods, accessors)
                .byteCode();
    }

    public String scriptText() {
        return new GroovyInterfaceImplGenerator(scriptName, clazzInterface, beanStubMethods).scriptText();
    }

}
