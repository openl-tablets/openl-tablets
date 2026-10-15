package org.openl.rules.ruleservice.core;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.springframework.asm.ClassReader;
import org.springframework.core.SmartClassLoader;

/**
 * The bean class loader of a service Spring context. Spring defines the CGLIB proxy classes of the context through it.
 *
 * <p>Spring calls {@link #publicDefineClass}, so a class-based proxy, such as that of a JDK class, needs no
 * {@code --add-opens=java.base/java.lang=ALL-UNNAMED}. Spring finds the method by reflection, so the class is public.
 *
 * <p>A proxy is defined beside the class it extends, in its class loader and package, as Spring does without this class
 * loader, so it intercepts package-private methods too. This class loader then finds the proxy by name, even when it
 * cannot reach that class loader otherwise, as with a Groovy class compiled from another script.
 *
 * <p>A proxy that cannot go beside the class it extends is defined here and intercepts only public and protected
 * methods: a proxy of a class of a module that does not open its package, such as a JDK class, a class Spring names in
 * another package, and a class the class loader of the extended class cannot link.
 *
 * <p>When another service has already defined a proxy of that name beside the same class, that proxy is reused if it
 * was generated the same way, from the same byte code, as Spring does. Otherwise this service gets its own proxy,
 * defined here.
 *
 * <p>Other classes and resources come from the parent class loader, found the way its own classes find them.
 */
public final class ProxyDefiningClassLoader extends ClassLoader implements SmartClassLoader {

    /**
     * The byte code of every proxy defined beside the class it extends, to tell whether another service can reuse it.
     */
    private static final Map<Class<?>, byte[]> BYTE_CODE_OF_PROXIES = Collections.synchronizedMap(new WeakHashMap<>());

    private final Map<String, Class<?>> proxiesDefinedBeside = new ConcurrentHashMap<>();

    ProxyDefiningClassLoader(ClassLoader parent) {
        super("service-spring-context", parent);
    }

    @Override
    public Class<?> publicDefineClass(String name, byte[] b, @Nullable ProtectionDomain protectionDomain) {
        var superclass = findSuperclass(new ClassReader(b).getSuperName().replace('/', '.'));
        var proxy = superclass != null ? defineBeside(superclass, name, b) : null;
        if (proxy == null) {
            return defineClass(name, b, 0, b.length, protectionDomain);
        }
        proxiesDefinedBeside.put(name, proxy);
        return proxy;
    }

    /**
     * Defines a class in the class loader and package of its superclass. Returns {@code null} when it cannot go there.
     */
    private static @Nullable Class<?> defineBeside(Class<?> superclass, String name, byte[] b) {
        try {
            var proxy = MethodHandles.privateLookupIn(superclass, MethodHandles.lookup()).defineClass(b);
            BYTE_CODE_OF_PROXIES.put(proxy, b);
            return proxy;
        } catch (IllegalAccessException | IllegalArgumentException e) {
            // The package is not open to this class loader, or the class is named in another package.
            return null;
        } catch (LinkageError e) {
            // Another service has defined a proxy of that name there, or the class loader cannot link this one.
            return findSameProxy(superclass.getClassLoader(), name, b);
        }
    }

    /**
     * Finds the proxy of that name that another service defined from the same byte code, or returns {@code null}.
     */
    private static @Nullable Class<?> findSameProxy(@Nullable ClassLoader classLoader, String name, byte[] b) {
        try {
            var defined = Class.forName(name, false, classLoader);
            return Arrays.equals(BYTE_CODE_OF_PROXIES.get(defined), b) ? defined : null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private @Nullable Class<?> findSuperclass(String name) {
        try {
            return Class.forName(name, false, getParent());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        var proxy = proxiesDefinedBeside.get(name);
        if (proxy != null) {
            return proxy;
        }
        var loaded = findLoadedClass(name);
        // Only so a Groovy class loader also finds the dependencies of an OpenL project and the classes defined in it.
        return loaded != null ? loaded : Class.forName(name, false, getParent());
    }

    @Override
    public @Nullable InputStream getResourceAsStream(String name) {
        // The parent closes the files it opens when it is closed.
        return getParent().getResourceAsStream(name);
    }
}
