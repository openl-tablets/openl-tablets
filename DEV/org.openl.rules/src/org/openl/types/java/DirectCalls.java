package org.openl.types.java;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.classloader.OpenLClassLoader;
import org.openl.util.ClassUtils;

/**
 * Generates small classes that call a Java method directly, without reflection.
 * <p>
 * A generated class is a {@link BiFunction} of the target and the array of arguments, never {@code null}. It casts them
 * to the types of the method, unboxes the primitive ones and boxes a primitive result; a {@code void} method gives
 * {@code null}. When the target, the number of the arguments or an argument does not fit, a {@code null} for a
 * primitive among them, it calls nothing and answers the array of arguments itself, which no method it calls has
 * seen: the caller then calls the method by reflection, which converts or rejects such arguments.
 * <p>
 * A method of a class this engine sees is called from a hidden class, which is unloaded together with its user. A
 * method of a class in an {@link OpenLClassLoader}, such as a datatype bean, is called from a class defined in that
 * class loader. No class is generated for a method that is not public, of a class that is not public, or of a JDK
 * class, whose methods may tell their caller.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
final class DirectCalls {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final ClassLoader OWN_LOADER = DirectCalls.class.getClassLoader();
    private static final AtomicLong COUNTER = new AtomicLong();

    private static final String HIDDEN_NAME = DirectCalls.class.getPackageName().replace('.', '/') + "/DirectCall";
    private static final Type OBJECT = Type.getType(Object.class);
    private static final Type OBJECT_ARRAY = Type.getType(Object[].class);
    private static final String FUNCTION = Type.getInternalName(BiFunction.class);
    private static final org.objectweb.asm.commons.Method CONSTRUCTOR = org.objectweb.asm.commons.Method
            .getMethod("void <init>()");
    private static final org.objectweb.asm.commons.Method APPLY = org.objectweb.asm.commons.Method
            .getMethod("Object apply(Object, Object)");

    /**
     * A direct call of {@code method}, or {@code null} when none can be made.
     */
    @SuppressWarnings("unchecked")
    static @Nullable BiFunction<Object, Object[], Object> of(Method method) {
        if (!isCallable(method)) {
            return null;
        }
        var owner = method.getDeclaringClass();
        try {
            Class<?> callClass;
            if (isVisible(method)) {
                callClass = LOOKUP.defineHiddenClass(generate(method, HIDDEN_NAME), true).lookupClass();
            } else if (owner.getClassLoader() instanceof OpenLClassLoader loader) {
                var name = owner.getName() + "$OpenLCall" + COUNTER.incrementAndGet();
                callClass = ClassLoaderUtils.defineClass(name, generate(method, name.replace('.', '/')), loader);
            } else {
                return null;
            }
            return (BiFunction<Object, Object[], Object>) callClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            // Whatever fails here, the method can still be called by reflection
            log.debug("Method '{}' is called by reflection: no direct call can be made to it.", method, e);
            return null;
        }
    }

    private static boolean isCallable(Method method) {
        var owner = method.getDeclaringClass();
        var loader = owner.getClassLoader();
        var jdk = loader == null || loader == ClassLoader.getPlatformClassLoader();
        var exported = owner.getModule().isExported(owner.getPackageName(), DirectCalls.class.getModule());
        return !jdk && exported && !owner.isHidden() && Modifier.isPublic(method.getModifiers())
                && Modifier.isPublic(owner.getModifiers());
    }

    /**
     * Whether the class loader of this class finds every type the method names: the hidden class resolves them there.
     */
    private static boolean isVisible(Method method) {
        if (!isVisible(method.getDeclaringClass()) || !isVisible(method.getReturnType())) {
            return false;
        }
        for (var type : method.getParameterTypes()) {
            if (!isVisible(type)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isVisible(Class<?> type) {
        var element = type;
        while (element.isArray()) {
            element = element.getComponentType();
        }
        if (element.isPrimitive()) {
            return true;
        }
        try {
            return Class.forName(element.getName(), false, OWN_LOADER) == element;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static byte[] generate(Method method, String internalName) {
        // The frames hold only this, the two Object parameters and an empty stack: no types to merge
        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return OBJECT.getInternalName();
            }
        };
        writer.visit(Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                internalName,
                null,
                OBJECT.getInternalName(),
                new String[]{FUNCTION});

        var init = new GeneratorAdapter(Opcodes.ACC_PUBLIC, CONSTRUCTOR, null, null, writer);
        init.loadThis();
        init.invokeConstructor(OBJECT, CONSTRUCTOR);
        init.returnValue();
        init.endMethod();

        var apply = new GeneratorAdapter(Opcodes.ACC_PUBLIC, APPLY, null, null, writer);
        var mismatch = apply.newLabel();
        checkArguments(apply, method, mismatch);
        callMethod(apply, method);
        apply.returnValue();
        apply.mark(mismatch);
        apply.loadArg(1);
        apply.returnValue();
        apply.endMethod();
        writer.visitEnd();
        return writer.toByteArray();
    }

    /**
     * Jumps to {@code mismatch} unless the target and the arguments are of the types the call casts them to.
     */
    private static void checkArguments(GeneratorAdapter apply, Method method, Label mismatch) {
        var owner = method.getDeclaringClass();
        if (!Modifier.isStatic(method.getModifiers())) {
            apply.loadArg(0);
            apply.instanceOf(Type.getType(owner));
            apply.ifZCmp(GeneratorAdapter.EQ, mismatch);
        }
        var parameterTypes = method.getParameterTypes();
        loadArguments(apply);
        apply.arrayLength();
        apply.push(parameterTypes.length);
        apply.ifICmp(GeneratorAdapter.NE, mismatch);
        for (var i = 0; i < parameterTypes.length; i++) {
            var type = parameterTypes[i];
            if (type != Object.class) {
                var next = apply.newLabel();
                loadArgument(apply, i);
                if (!type.isPrimitive()) {
                    // A null fits a reference parameter; a primitive one takes no null
                    apply.ifNull(next);
                    loadArgument(apply, i);
                }
                apply.instanceOf(Type.getType(ClassUtils.primitiveToWrapper(type)));
                apply.ifZCmp(GeneratorAdapter.EQ, mismatch);
                apply.mark(next);
            }
        }
    }

    private static void callMethod(GeneratorAdapter apply, Method method) {
        var owner = method.getDeclaringClass();
        var ownerType = Type.getType(owner);
        var isStatic = Modifier.isStatic(method.getModifiers());
        if (!isStatic) {
            apply.loadArg(0);
            apply.checkCast(ownerType);
        }
        var parameterTypes = method.getParameterTypes();
        for (var i = 0; i < parameterTypes.length; i++) {
            loadArgument(apply, i);
            unbox(apply, parameterTypes[i]);
        }
        int opcode;
        if (isStatic) {
            opcode = Opcodes.INVOKESTATIC;
        } else if (owner.isInterface()) {
            opcode = Opcodes.INVOKEINTERFACE;
        } else {
            opcode = Opcodes.INVOKEVIRTUAL;
        }
        apply.visitMethodInsn(opcode,
                ownerType.getInternalName(),
                method.getName(),
                Type.getMethodDescriptor(method),
                owner.isInterface());
        // Boxes a primitive result with valueOf, as reflection does, and gives null for void
        apply.valueOf(Type.getType(method.getReturnType()));
    }

    private static void loadArguments(GeneratorAdapter apply) {
        apply.loadArg(1);
        apply.checkCast(OBJECT_ARRAY);
    }

    private static void loadArgument(GeneratorAdapter apply, int i) {
        loadArguments(apply);
        apply.push(i);
        apply.arrayLoad(OBJECT);
    }

    /**
     * Casts the argument on the stack to the parameter type, unboxing a primitive from its exact wrapper.
     */
    private static void unbox(GeneratorAdapter apply, Class<?> parameterType) {
        if (parameterType.isPrimitive()) {
            var wrapper = Type.getType(ClassUtils.primitiveToWrapper(parameterType));
            var primitive = Type.getType(parameterType);
            apply.checkCast(wrapper);
            apply.invokeVirtual(wrapper,
                    new org.objectweb.asm.commons.Method(parameterType.getName() + "Value", primitive, new Type[0]));
        } else if (parameterType != Object.class) {
            apply.checkCast(Type.getType(parameterType));
        }
    }
}
