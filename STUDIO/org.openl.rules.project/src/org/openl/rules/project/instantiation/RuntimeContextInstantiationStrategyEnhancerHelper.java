package org.openl.rules.project.instantiation;

import java.lang.reflect.Method;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author PUdalau, Marat Kamalov
 */
public final class RuntimeContextInstantiationStrategyEnhancerHelper {

    private static final String RUNTIME_CONTEXT = "Lorg/openl/rules/context/IRulesRuntimeContext;";

    private RuntimeContextInstantiationStrategyEnhancerHelper() {
    }

    /**
     * Suffix of undecorated class name.
     */
    private static final String UNDECORATED_CLASS_NAME_SUFFIX = "$RuntimeContextUndecorated";
    /**
     * Suffix of decorated class name.
     */
    private static final String DECORATED_CLASS_NAME_SUFFIX = "$RuntimeContextDecorated";

    public static boolean isDecoratedClass(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (method.getParameterTypes().length == 0 || method.getParameterTypes()[0] != IRulesRuntimeContext.class) {
                return false;
            }
        }
        return true;
    }

    /**
     * Undecorates methods signatures of given class.
     *
     * @param clazz interface to undecorate
     * @param classLoader The classloader where generated class should be placed.
     * @return new class with undecorated methods signatures: removed {@link IRulesRuntimeContext} as the first
     *         parameter for each method.
     * @throws Exception
     */
    public static Class<?> undecorateClass(Class<?> clazz, ClassLoader classLoader) throws Exception {
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException("Supports only interface classes.");
        }

        final String className = clazz.getName() + UNDECORATED_CLASS_NAME_SUFFIX;
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            Logger log = LoggerFactory.getLogger(RuntimeContextInstantiationStrategyEnhancerHelper.class);
            log.debug("Generating interface without runtime context for '{}' class.", clazz.getName());

            return innerUndecorateInterface(className, clazz, classLoader);
        }
    }

    private static Class<?> innerUndecorateInterface(String className,
            Class<?> original,
            ClassLoader classLoader) throws Exception {

        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new UndecoratingClassWriter(classWriter, className);
        InterfaceTransformer transformer = new InterfaceTransformer(original, className, false);
        transformer.accept(classVisitor);
        classWriter.visitEnd();

        // Create class object.
        //
        ClassUtils.defineClass(className, classWriter.toByteArray(), classLoader);

        // Return loaded to classpath class object.
        //
        return Class.forName(className, true, classLoader);
    }

    /**
     * Decorates methods signatures of given clazz.
     *
     * @param clazz class to decorate
     * @param classLoader The classloader where generated class should be placed.
     * @return new class with decorated methods signatures: added {@link IRulesRuntimeContext} as the first parameter
     *         for each method.
     * @throws Exception
     */
    public static Class<?> decorateClass(Class<?> clazz, ClassLoader classLoader) throws Exception {
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException("Supports only interface classes.");
        }

        final String className = clazz.getName() + DECORATED_CLASS_NAME_SUFFIX;
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            Logger log = LoggerFactory.getLogger(RuntimeContextInstantiationStrategyEnhancerHelper.class);
            log.debug("Generating interface with runtime context for '{}' class.", clazz.getName());

            return innerDecorateInterface(className, clazz, classLoader);
        }
    }

    private static Class<?> innerDecorateInterface(String className,
            Class<?> original,
            ClassLoader classLoader) throws Exception {

        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new DecoratingClassWriter(classWriter, className);
        InterfaceTransformer transformer = new InterfaceTransformer(original, className, false);
        transformer.accept(classVisitor);
        classWriter.visitEnd();

        // Create class object.
        //
        ClassUtils.defineClass(className, classWriter.toByteArray(), classLoader);

        // Return loaded to classpath class object.
        //
        return Class.forName(className, true, classLoader);
    }

    static class DecoratingClassWriter extends ClassVisitor {
        private final String className;

        public DecoratingClassWriter(ClassVisitor delegatedClassVisitor, String className) {
            super(Opcodes.ASM5, delegatedClassVisitor);
            this.className = className;
        }

        @Override
        public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) {
            super.visit(arg0, arg1, className.replace('.', '/'), arg3, arg4, arg5);
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String arg1, String arg2, String arg3, String[] arg4) {
            return super.visitMethod(arg0, arg1, addRuntimeContextFromSignature(arg2), arg3, arg4);
        }

        private String addRuntimeContextFromSignature(String signature) {
            return "(" + RUNTIME_CONTEXT + signature.substring(1);
        }
    }

    /**
     * {@link ClassWriter} for creation undecorated class: for removing {@link IRulesRuntimeContext} from signature of
     * each method.
     *
     * @author PUdalau
     */
    static class UndecoratingClassWriter extends ClassVisitor {
        private final String className;

        public UndecoratingClassWriter(ClassVisitor delegatedClassVisitor, String className) {
            super(Opcodes.ASM5, delegatedClassVisitor);
            this.className = className;
        }

        @Override
        public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) {
            super.visit(arg0, arg1, className.replace('.', '/'), arg3, arg4, arg5);
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String arg1, String arg2, String arg3, String[] arg4) {
            return super.visitMethod(arg0, arg1, removeRuntimeContextFromSignature(arg2), arg3, arg4);
        }

        private String removeRuntimeContextFromSignature(String signature) {
            if (signature.startsWith("(" + RUNTIME_CONTEXT)) {
                return "(" + signature.substring(RUNTIME_CONTEXT.length() + 1);
            } else {
                throw new IllegalArgumentException("IRulesRuntimeContext is expected in signature.");
            }
        }
    }
}
