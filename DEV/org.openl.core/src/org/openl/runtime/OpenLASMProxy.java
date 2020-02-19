package org.openl.runtime;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.util.ClassUtils;

public final class OpenLASMProxy {

    private final static AtomicInteger nameCounter = new AtomicInteger(0);
    private static final String HANDLER = "openLProxyHandler";
    private static final Type HANDLER_TYPE = Type.getType(OpenLProxyHandler.class);
    private static final Method INVOKE_HANDLER = Method.getMethod(OpenLProxyHandler.class.getDeclaredMethods()[0]);
    private static final Type CLASS_TYPE = Type.getType(Class.class);
    private static final Method GET_METHOD = Method
        .getMethod("java.lang.reflect.Method getMethod(java.lang.String, java.lang.Class[])");
    private static final Method GET_HANDLER = Method.getMethod(OpenLProxy.class.getDeclaredMethods()[0]);

    private OpenLASMProxy() {
    }

    public static <T> T newProxyInstance(ClassLoader classLoader, OpenLProxyHandler handler, Class<T> proxyInterface) {
        @SuppressWarnings("unchecked")
        T proxyInstance = (T) newProxyInstance(classLoader, handler, new Class[] { proxyInterface });
        return proxyInstance;

    }

    public static Object newProxyInstance(ClassLoader classLoader, OpenLProxyHandler handler, Class<?>... interfaces) {
        String proxyClassName = Type.getInternalName(interfaces[0]) + "$proxy" + nameCounter.incrementAndGet();
        Type proxyType = Type.getObjectType(proxyClassName);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        List<Class<?>> listInterfaces = Arrays.stream(interfaces).collect(Collectors.toList());
        if (!listInterfaces.contains(OpenLProxy.class)) {
            listInterfaces.add(OpenLProxy.class);
        }
        cw.visit(Opcodes.V1_8,
            Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_FINAL,
            proxyType.getInternalName(),
            null,
            Type.getInternalName(Object.class),
            listInterfaces.stream().map(Type::getInternalName).toArray(String[]::new));
        FieldVisitor fv = cw
            .visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL, HANDLER, HANDLER_TYPE.getDescriptor(), null, null);
        fv.visitEnd();
        writeConstructor(cw, proxyType);
        writeHandlerGetter(cw, proxyType);
        HashSet<Method> methods = new HashSet<>();
        for (Class<?> proxyInterface : interfaces) {
            if (!proxyInterface.getName().equals(OpenLProxy.class.getName())) {
                Type interfaceType = Type.getType(proxyInterface);
                for (java.lang.reflect.Method method : proxyInterface.getMethods()) {
                    Method m = Method.getMethod(method);
                    if (!methods.contains(m)) {
                        methods.add(m);
                        writeMethods(cw, m, proxyType, interfaceType);
                    }
                }
            }
        }
        cw.visitEnd();
        byte[] bytes = cw.toByteArray();
        try {
            Class<?> aClass = ClassUtils.defineClass(proxyType.getClassName(), bytes, classLoader);
            return aClass.getDeclaredConstructor(OpenLProxyHandler.class).newInstance(handler);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to instantiate a new proxy.", e);
        }
    }

    private static void writeConstructor(ClassWriter cw, Type name) {
        GeneratorAdapter mv = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
            Method.getMethod("void <init>(org.openl.runtime.OpenLProxyHandler)"),
            null,
            null,
            cw);
        mv.visitCode();
        mv.loadThis();
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
        mv.loadThis();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.putField(name, HANDLER, HANDLER_TYPE);
        mv.returnValue();
        mv.endMethod();
    }

    private static void writeHandlerGetter(ClassWriter cw, Type name) {
        GeneratorAdapter mv = new GeneratorAdapter(Opcodes.ACC_PUBLIC, GET_HANDLER, null, null, cw);
        mv.visitCode();
        mv.loadThis();
        mv.getField(name, HANDLER, HANDLER_TYPE);
        mv.returnValue();
        mv.endMethod();
    }

    private static void writeMethods(ClassWriter cw, Method method, Type name, Type proxyInterface) {
        GeneratorAdapter mv = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
            method,
            null,
            new Type[] { Type.getType(Exception.class) },
            cw);
        mv.visitCode();
        mv.loadThis();
        mv.getField(name, HANDLER, HANDLER_TYPE);
        mv.loadThis();
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
        return o instanceof OpenLProxy;
    }

    public static OpenLProxyHandler getHandler(Object o) {
        if (isProxy(o)) {
            return ((OpenLProxy) o).getHandler();
        } else {
            throw new IllegalArgumentException(
                String.format("Expected to be proxied using '%s' class", OpenLASMProxy.class.getTypeName()));
        }
    }
}
