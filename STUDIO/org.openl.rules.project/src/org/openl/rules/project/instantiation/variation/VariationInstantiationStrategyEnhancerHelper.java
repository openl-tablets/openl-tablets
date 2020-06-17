package org.openl.rules.project.instantiation.variation;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.rules.variation.VariationsPack;
import org.openl.rules.variation.VariationsResult;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods related to the variations injection into interface of rules.
 *
 * @author PUdalau, Marat Kamalov
 */
public final class VariationInstantiationStrategyEnhancerHelper {
    private static final String VARIATIONS_PACK_TYPE = "Lorg/openl/rules/variation/VariationsPack;";
    private static final String VARIATIONS_RESULT_TYPE = "Lorg/openl/rules/variation/VariationsResult;";

    private VariationInstantiationStrategyEnhancerHelper() {
    }

    /**
     * Suffix of decorated class name.
     */
    private static final String UNDECORATED_CLASS_NAME_SUFFIX = "$VariationsUndecorated";
    /**
     * Suffix of undecorated class name.
     */
    private static final String DECORATED_CLASS_NAME_SUFFIX = "$VariationsDecorated";

    /**
     * Checks whether the specified interface was enhanced with variations or not(if there exists at least one enhanced
     * method).
     *
     * @param clazz Interface to check.
     * @return <code>true</code> if at least one method of interface is method for calculations with variations and
     *         <code>false</code> otherwise.
     */
    public static boolean isDecoratedClass(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (isDecoratedMethod(method)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the specified method is enhanced with variations.
     *
     * @param method The method to check.
     * @return <code>true</code> if method has the {@link VariationsPack} as the last parameter and returns
     *         {@link VariationsResult} and <code>false</code> otherwise.
     */
    public static boolean isDecoratedMethod(Method method) {
        int paramsLength = method.getParameterTypes().length;
        return paramsLength != 0 && method.getParameterTypes()[paramsLength - 1] == VariationsPack.class && method
            .getReturnType()
            .equals(VariationsResult.class);
    }

    /**
     * Undecorates methods signatures of given clazz. Undecoration implies that all methods that was enhanced with
     * variations will be removed from servce class.
     *
     * @param clazz class to undecorate
     * @param classLoader The classloader where generated class should be placed.
     * @return new class with undecorated methods signatures
     * @throws Exception
     */
    public static Class<?> undecorateClass(Class<?> clazz, ClassLoader classLoader) throws Exception {
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException("Only interface classes are supported.");
        }

        final String className = clazz.getName() + UNDECORATED_CLASS_NAME_SUFFIX;
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            Logger log = LoggerFactory.getLogger(VariationInstantiationStrategyEnhancerHelper.class);
            log.debug("Generating interface without variations for '{}' class", clazz.getName());

            return innerUndecorateInterface(className, clazz, classLoader);
        }
    }

    private static Class<?> innerUndecorateInterface(String className,
            Class<?> original,
            ClassLoader classLoader) throws Exception {
        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new UndecoratingClassWriter(classWriter, className);
        InterfaceTransformer transformer = new InterfaceTransformer(original, className);
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
     * Decorates methods signatures of given clazz. New decorated class will have both original methods and decorated
     * methods with {@link VariationsPack} as the last parameter and {@link VariationsResult} as the return type.
     *
     * @param clazz class to decorate
     * @param classLoader The classloader where generated class should be placed.
     * @return new class with decorated methods signatures
     * @throws Exception
     */
    public static Class<?> decorateClass(Class<?> clazz, ClassLoader classLoader) throws Exception {
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException("Only interface classes are supported.");
        }

        final String className = clazz.getName() + DECORATED_CLASS_NAME_SUFFIX;
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            Logger log = LoggerFactory.getLogger(VariationInstantiationStrategyEnhancerHelper.class);
            log.debug("Generating interface with variations for '{}' class", clazz.getName());
            return innerDecorateInterface(className, clazz, classLoader);
        }
    }

    private static Class<?> innerDecorateInterface(String className,
            Class<?> original,
            ClassLoader classLoader) throws Exception {
        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new DecoratingClassWriter(classWriter, className);
        InterfaceTransformer transformer = new InterfaceTransformer(original, className);
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
     * Searches for method that will be executed instead of method in enhanced interface.
     *
     * @param simpleClass Class without variations injection.
     * @param decoratedMethod Method enhanced with variations.
     * @return Corresponding method in original interface for method from enhanced interface.
     * @throws Exception Possible exception from java reflection caused wrong method accessing.
     */
    public static Method getMethodForDecoration(Class<?> simpleClass, Method decoratedMethod) throws Exception {
        Class<?>[] parameterTypes = decoratedMethod.getParameterTypes();
        if (VariationInstantiationStrategyEnhancerHelper.isDecoratedMethod(decoratedMethod)) {
            return simpleClass.getMethod(decoratedMethod.getName(),
                Arrays.copyOf(parameterTypes, parameterTypes.length - 1));
        } else {
            return simpleClass.getMethod(decoratedMethod.getName(), parameterTypes);
        }
    }

    private static class DecoratingClassWriter extends ClassVisitor {
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
            super.visitMethod(arg0, arg1, addVariationToSignature(arg2), arg3, arg4);
            return super.visitMethod(arg0, arg1, arg2, arg3, arg4);
        }

        private String addVariationToSignature(String signature) {
            return signature.substring(0, signature.indexOf(")")) + VARIATIONS_PACK_TYPE + ")" + VARIATIONS_RESULT_TYPE;
        }
    }

    // FIXME skip decorated methods
    /**
     * {@link ClassWriter} for creation undecorated class: all decorated with variations methods will be removed from
     * interface.
     *
     * @author PUdalau
     */
    private static class UndecoratingClassWriter extends ClassVisitor {
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
            // write only undecorated method
            if (!isDecoratedMethod(arg2)) {
                return super.visitMethod(arg0, arg1, arg2, arg3, arg4);
            } else {
                return null;// null means skip this method
            }
        }

        private boolean isDecoratedMethod(String signature) {
            return signature.contains(VARIATIONS_PACK_TYPE + ")") && signature.endsWith(VARIATIONS_RESULT_TYPE);
        }

    }
}
