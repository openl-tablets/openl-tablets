package org.openl.rules.project.instantiation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.runtime.InterfaceGenerator;
import org.openl.rules.runtime.RuleInfo;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author PUdalau, Marat Kamalov
 */
public final class RuntimeContextInstantiationStrategyEnhancerHelper {

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

        final Logger log = LoggerFactory.getLogger(RuntimeContextInstantiationStrategyEnhancerHelper.class);

        String className = clazz.getName() + UNDECORATED_CLASS_NAME_SUFFIX;

        log.debug("Generating interface without runtime context for '{}' class.", clazz.getName());

        return innerUndecorateInterface(className, clazz, classLoader);
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
     * TODO: replace with a configurable implementation
     * <p/>
     * <p/>
     * Check that method should be ignored by enhancer.
     *
     * @param method method to check
     * @return <code>true</code> if method should be ignored; <code>false</code> - otherwise
     */
    private static boolean isIgnored(Method method) {
        // Ignore methods what are inherited from Object.class
        // Note that ignored inherited methods only.
        return ArrayUtils.contains(Object.class.getMethods(), method);
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
        final Logger log = LoggerFactory.getLogger(RuntimeContextInstantiationStrategyEnhancerHelper.class);

        Method[] methods = clazz.getMethods();
        List<RuleInfo> rules = getRulesDecorated(methods);

        String className = clazz.getName() + DECORATED_CLASS_NAME_SUFFIX;
        RuleInfo[] rulesArray = rules.toArray(new RuleInfo[rules.size()]);

        log.debug("Generating interface for '{}' class", clazz.getName());

        return InterfaceGenerator.generateInterface(className, rulesArray, classLoader);
    }

    /**
     * Gets list of rules.
     *
     * @param methods array of methods what represents rule methods
     * @return list of rules meta-info
     */
    private static List<RuleInfo> getRulesDecorated(Method[] methods) {

        List<RuleInfo> rules = new ArrayList<>(methods.length);

        for (Method method : methods) {

            // Check that method should be ignored or not.
            if (isIgnored(method)) {
                continue;
            }

            String methodName = method.getName();

            Class<?>[] paramTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            Class<?>[] newParams = new Class<?>[] { IRulesRuntimeContext.class };
            Class<?>[] extendedParamTypes = ArrayUtils.addAll(newParams, paramTypes);

            RuleInfo ruleInfo = InterfaceGenerator.createRuleInfo(methodName, extendedParamTypes, returnType);

            rules.add(ruleInfo);
        }

        return rules;
    }

    /**
     * {@link ClassWriter} for creation undecorated class: for removing {@link IRulesRuntimeContext} from signature of
     * each method.
     *
     * @author PUdalau
     */
    static class UndecoratingClassWriter extends ClassVisitor {

        private static final String RUNTIME_CONTEXT = "Lorg/openl/rules/context/IRulesRuntimeContext;";
        private String className;

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
                throw new IllegalArgumentException("IRulesRuntimeContext has been expected in signature.");
            }
        }
    }
}
