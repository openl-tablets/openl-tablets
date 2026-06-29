package org.openl.runtime;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.exception.OpenlNotCheckedException;

public final class ASMProxyFactory {

    private static final String PROXY_SUFFIX = "$$Proxy";
    // Guards the rare first-time generation of a proxy class so two threads never define the same one twice.
    private static final Object PROXY_GENERATION_LOCK = new Object();
    private static final String HANDLER = "_handler";
    private static final Type HANDLER_TYPE = Type.getType(ASMProxyHandler.class);
    private static final Method INVOKE_HANDLER = Method.getMethod(ASMProxyHandler.class.getDeclaredMethods()[0]);
    private static final Type CLASS_TYPE = Type.getType(Class.class);
    private static final Method GET_METHOD = Method
            .getMethod("java.lang.reflect.Method getMethod(java.lang.String, java.lang.Class[])");

    private ASMProxyFactory() {
    }

    public static <T> T newProxyInstance(ClassLoader classLoader, ASMProxyHandler handler, Class<T> proxyInterface) {
        @SuppressWarnings("unchecked")
        T proxyInstance = (T) newProxyInstance(classLoader, handler, new Class[]{proxyInterface});
        return proxyInstance;

    }

    public static Object newProxyInstance(ClassLoader classLoader, ASMProxyHandler handler, Class<?>... interfaces) {
        try {
            var proxyClass = getProxyClass(classLoader, interfaces);
            return proxyClass.getDeclaredConstructor(ASMProxyHandler.class).newInstance(handler);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to instantiate a new proxy.", e);
        }
    }

    /**
     * Returns the proxy class for the given interfaces, generating it on the first request and reusing it afterwards.
     * The proxy class is fully defined by the proxied interfaces, so generating a uniquely named class on every call
     * would pollute the (long-lived) classloader's Metaspace with identical classes (see issue #1230).
     */
    private static Class<?> getProxyClass(ClassLoader classLoader, Class<?>[] interfaces) throws ClassNotFoundException {
        var proxyClassName = proxyClassName(interfaces);
        try {
            return classLoader.loadClass(proxyClassName);
        } catch (ClassNotFoundException e) {
            // Serialize the one-time generation so concurrent first calls do not define the same class twice.
            synchronized (PROXY_GENERATION_LOCK) {
                try {
                    return classLoader.loadClass(proxyClassName);
                } catch (ClassNotFoundException notDefinedYet) {
                    var bytes = generateProxyByteCode(proxyClassName, interfaces);
                    return ClassLoaderUtils.defineClass(proxyClassName, bytes, classLoader);
                }
            }
        }
    }

    /**
     * Builds a deterministic proxy class name from the proxied interfaces so that repeated requests resolve to the
     * same generated class. The name stays in the package of the first interface. The {@code $$Proxy} marker keeps it
     * from clashing with a real (possibly nested) class. Each additional interface is appended through a reversible
     * escape so that distinct interface sets can never map to the same name.
     */
    static String proxyClassName(Class<?>[] interfaces) {
        var sb = new StringBuilder(interfaces[0].getName()).append(PROXY_SUFFIX);
        for (int i = 1; i < interfaces.length; i++) {
            sb.append('$').append(escape(interfaces[i].getName()));
        }
        return sb.toString();
    }

    /**
     * Reversibly encodes a binary class name into a single class-name segment. The dot, dollar and underscore
     * characters - which all legitimately appear in binary names - are escaped to distinct two-character sequences, so
     * the result is collision-free, contains neither {@code '.'} nor {@code '$'}, and can be safely embedded in a
     * proxy class name and delimited with {@code '$'}.
     */
    static String escape(String binaryName) {
        var sb = new StringBuilder(binaryName.length() + 4);
        for (int i = 0; i < binaryName.length(); i++) {
            var c = binaryName.charAt(i);
            switch (c) {
                case '_' -> sb.append("_u");
                case '.' -> sb.append("_d");
                case '$' -> sb.append("_s");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static byte[] generateProxyByteCode(String proxyClassName, Class<?>[] interfaces) {
        var proxyType = Type.getObjectType(proxyClassName.replace('.', '/'));
        var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        var listInterfaces = Arrays.stream(interfaces).collect(Collectors.toList());
        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_FINAL,
                proxyType.getInternalName(),
                null,
                Type.getInternalName(ASMProxy.class),
                listInterfaces.stream().map(Type::getInternalName).toArray(String[]::new));
        writeConstructor(cw, proxyType);
        var methods = new HashSet<Method>();
        for (var proxyInterface : interfaces) {
            var interfaceType = Type.getType(proxyInterface);
            for (var method : proxyInterface.getMethods()) {
                var m = Method.getMethod(method);
                if (methods.add(m)) {
                    writeMethods(cw, m, method, proxyType, interfaceType);
                }
            }
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void writeConstructor(ClassWriter cw, Type name) {
        GeneratorAdapter mv = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
                Method.getMethod("void <init>(" + ASMProxyHandler.class.getName() + ")"),
                null,
                null,
                cw);
        mv.visitCode();
        mv.loadThis();
        mv.invokeConstructor(Type.getType(ASMProxy.class), Method.getMethod("void <init>()"));
        mv.loadThis();
        mv.loadArg(0);
        mv.putField(name, HANDLER, HANDLER_TYPE);
        mv.returnValue();
        mv.endMethod();
    }

    private static void writeMethods(ClassWriter cw, Method method, java.lang.reflect.Method m, Type name, Type proxyInterface) {
        GeneratorAdapter mv = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
                method,
                null,
                new Type[]{Type.getType(Exception.class)},
                cw);
        for (Parameter p : m.getParameters()) {
            mv.visitParameter(p.getName(), Opcodes.ACC_FINAL);
        }
        mv.visitCode();
        mv.loadThis();
        mv.getField(name, HANDLER, HANDLER_TYPE);
        mv.push(proxyInterface);
        mv.push(method.getName());
        mv.push(method.getArgumentTypes().length);
        mv.newArray(CLASS_TYPE);
        Type[] parameterTypes = method.getArgumentTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            mv.dup();
            mv.push(i);
            mv.push(parameterTypes[i]);
            mv.arrayStore(CLASS_TYPE);
        }
        mv.invokeVirtual(CLASS_TYPE, GET_METHOD);
        mv.loadArgArray();
        mv.invokeInterface(HANDLER_TYPE, INVOKE_HANDLER);
        mv.unbox(mv.getReturnType());
        mv.returnValue();
        mv.endMethod();
    }

    public static boolean isProxy(Object o) {
        return o instanceof ASMProxy;
    }

    public static ASMProxyHandler getProxyHandler(Object o) {
        if (isProxy(o)) {
            return ((ASMProxy) o)._handler;
        } else {
            throw new IllegalArgumentException(
                    "Expected to be proxied using '%s' class".formatted(ASMProxyFactory.class.getTypeName()));
        }
    }
}
