package org.openl.runtime;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.openl.exception.OpenLRuntimeException;
import org.openl.util.ClassUtils;

public final class OpenLASMProxy {

    public static AtomicInteger nameCounter = new AtomicInteger(0);

    private OpenLASMProxy() {
    }

    public static Object create(ClassLoader classLoader, OpenLProxyHandler openLProxyHandler, Class[] interfaces) {
        String name = (OpenLASMProxy.class.getName() + nameCounter.incrementAndGet()).replaceAll("\\.", "/");
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        List<Class> listInterfaces = Arrays.stream(interfaces).collect(Collectors.toList());
        if (!listInterfaces.contains(OpenLProxy.class)) {
            listInterfaces.add(OpenLProxy.class);
        }
        cw.visit(Opcodes.V1_8,
            Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
            name,
            null,
            Type.getInternalName(Object.class),
            listInterfaces.stream().map(i -> Type.getInternalName(i)).toArray(String[]::new));
        FieldVisitor fv = cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL,
            "openLProxyHandler",
            "Lorg/openl/runtime/OpenLProxyHandler;",
            null,
            null);
        fv.visitEnd();
        createConstructor(cw, name);
        createHandlerGetter(cw, name);
        Map<String, String> methodsDescriptors = new HashMap<>();
        for (Class proxyInterface : interfaces) {
            if (!proxyInterface.getName().equals(OpenLProxy.class.getName())) {
                for (Method method : proxyInterface.getMethods()) {
                    if (!(methodsDescriptors.containsKey(method.getName()) && methodsDescriptors.get(method.getName())
                        .equals(Type.getMethodDescriptor(method)))) {
                        methodsDescriptors.put(method.getName(), Type.getMethodDescriptor(method));
                        createMethods(cw, method, name, proxyInterface);
                    }
                }
            }
        }
        cw.visitEnd();
        byte[] bytes = cw.toByteArray();
        Class<?> aClass = null;
        try {
            aClass = ClassUtils.defineClass(name.replaceAll("/", "."), bytes, classLoader);
            return aClass.getDeclaredConstructor(OpenLProxyHandler.class).newInstance(openLProxyHandler);
        } catch (Exception e) {
            throw new OpenLRuntimeException(e);
        }
    }

    public static void createConstructor(ClassWriter cw, String name) {
        MethodVisitor mv = cw
            .visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Lorg/openl/runtime/OpenLProxyHandler;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, name, "openLProxyHandler", "Lorg/openl/runtime/OpenLProxyHandler;");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public static void createHandlerGetter(ClassWriter cw, String name) {
        MethodVisitor mv = cw
            .visitMethod(Opcodes.ACC_PUBLIC, "getHandler", "()Lorg/openl/runtime/OpenLProxyHandler;", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, name, "openLProxyHandler", "Lorg/openl/runtime/OpenLProxyHandler;");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public static void createMethods(ClassWriter cw, Method method, String name, Class proxyInterface) {
        org.objectweb.asm.commons.Method method1 = org.objectweb.asm.commons.Method.getMethod(method);
        GeneratorAdapter mv = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
            method1,
            null,
            Arrays.stream(method.getExceptionTypes()).map(Type::getType).toArray(Type[]::new),
            cw);
        mv.visitCode();
        mv.loadThis();
        mv.visitFieldInsn(Opcodes.GETFIELD, name, "openLProxyHandler", "Lorg/openl/runtime/OpenLProxyHandler;");
        mv.loadThis();
        mv.push(Type.getType(proxyInterface));
        mv.push(method.getName());
        mv.push(method.getParameters().length);
        mv.newArray(Type.getType(Class.class));
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            mv.dup();
            mv.push(i);
            mv.push(Type.getType(parameterTypes[i]));
            mv.arrayStore(Type.getType(Class.class));
        }
        mv.invokeVirtual(Type.getType(Class.class),
            org.objectweb.asm.commons.Method
                .getMethod("java.lang.reflect.Method getMethod(java.lang.String, java.lang.Class[])"));
        mv.loadArgArray();
        mv.invokeInterface(Type.getType(OpenLProxyHandler.class),
            org.objectweb.asm.commons.Method
                .getMethod("java.lang.Object invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])"));
        mv.unbox(mv.getReturnType());
        mv.returnValue();
        mv.endMethod();
    }

    public static boolean isProxy(Object o) {
        return o instanceof OpenLProxy;
    }

    public static OpenLProxyHandler getHandler(Object o) {
        return ((OpenLProxy) o).getHandler();
    }

    public interface OpenLProxy {
        OpenLProxyHandler getHandler();
    }
}
