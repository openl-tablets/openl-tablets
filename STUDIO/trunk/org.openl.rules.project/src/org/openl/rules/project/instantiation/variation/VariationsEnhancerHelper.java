package org.openl.rules.project.instantiation.variation;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.cglib.core.ReflectUtils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.openl.rules.runtime.RuleInfo;
import org.openl.rules.runtime.RulesFactory;

/**
 * Utility methods related to the variations injection into interface of rules.
 * 
 * @author PUdalau
 */
public abstract class VariationsEnhancerHelper {

    /**
     * Suffix of enhanced class name.
     */
    private static final String UNDECORATED_CLASS_NAME_SUFFIX = "$WithoutVariations";
    /**
     * Suffix of enhanced class name.
     */
    private static final String ENHANCED_CLASS_NAME_SUFFIX = "$VariationsEnhanced";

    /**
     * Checks whether the specified interface was enhanced with variations or
     * not(if there exists at least one enhanced method).
     * 
     * @param rulesInterface Interface to check.
     * @return <code>true</code> if at least one method of interface is method
     *         for calculations with variations and <code>false</code>
     *         otherwise.
     */
    public static boolean isEnhancedClass(Class<?> rulesInterface) {
        for (Method method : rulesInterface.getMethods()) {
            if (isEnhancedMethod(method)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the specified method is enhanced with variations.
     * 
     * @param method The method to check.
     * @return <code>true</code> if method has the {@link VariationsPack} as the
     *         last parameter and returns {@link VariationsResult} and
     *         <code>false</code> otherwise.
     */
    public static boolean isEnhancedMethod(Method method) {
        int paramsLength = method.getParameterTypes().length;
        if (paramsLength == 0 || method.getParameterTypes()[paramsLength - 1] != VariationsPack.class || !method.getReturnType()
            .equals(VariationsResult.class)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Undecorates methods signatures of given clazz. Undecoration implies that
     * all methods that was enhanced with variations will be removed from servce
     * class.
     * 
     * 
     * @param clazz class to undecorate
     * @param classLoader The classloader where generated class should be
     *            placed.
     * @return new class with undecorated methods signatures
     * @throws Exception
     */
    public static Class<?> undecorateMethods(Class<?> clazz, ClassLoader classLoader) throws Exception {
    	final Log log = LogFactory.getLog(VariationsEnhancerHelper.class);
    	
        String className = clazz.getName() + UNDECORATED_CLASS_NAME_SUFFIX;

        log.debug(String.format("Generating proxy interface without runtime context for '%s' class", clazz.getName()));

        return undecorateInterface(className, clazz, classLoader);
    }

    private static InputStream getClassAsStream(Class<?> clazz, ClassLoader classLoader) {
        String name = clazz.getName().replace('.', '/') + ".class";
        return clazz.getClassLoader().getResourceAsStream(name);
    }

    private static Class<?> undecorateInterface(String className, Class<?> original, ClassLoader classLoader) throws Exception {

        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new UndecoratingClassWriter(classWriter, className);
        ClassReader classReader = new ClassReader(getClassAsStream(original, classLoader));
        classReader.accept(classVisitor, 0);
        // classWriter.visitEnd();

        // Create class object.
        //
        ReflectUtils.defineClass(className, classWriter.toByteArray(), classLoader);

        // Return loaded to classpath class object.
        //
        return Class.forName(className, true, classLoader);
    }

    /**
     * TODO: replace with a configurable implementation
     * 
     * 
     * Check that method should be ignored by enhancer.
     * 
     * @param method method to check
     * @return <code>true</code> if method should be ignored; <code>false</code>
     *         - otherwise
     */
    private static boolean isIgnored(Method method) {
        // Ignore methods what are inherited from Object.class
        // Note that ignored inherited methods only.
        return ArrayUtils.contains(Object.class.getMethods(), method);
    }

    /**
     * Decorates methods signatures of given clazz. New decorated class will
     * have both original methods and decorated methods with
     * {@link VariationsPack} as the last parameter and {@link VariationsResult}
     * as the return type.
     * 
     * @param clazz class to decorate
     * @param classLoader The classloader where generated class should be
     *            placed.
     * @return new class with decorated methods signatures
     * @throws Exception
     */
    public static Class<?> decorateMethods(Class<?> clazz, ClassLoader classLoader) throws Exception {
    	final Log log = LogFactory.getLog(VariationsEnhancerHelper.class);
    	
        Method[] methods = clazz.getMethods();
        List<RuleInfo> rules = getRulesDecorated(methods);

        String className = clazz.getName() + ENHANCED_CLASS_NAME_SUFFIX;
        RuleInfo[] rulesArray = rules.toArray(new RuleInfo[rules.size()]);

        log.debug(String.format("Generating proxy interface for '%s' class", clazz.getName()));

        return RulesFactory.generateInterface(className, rulesArray, classLoader);
    }

    /**
     * Gets list of rules.
     * 
     * @param methods array of methods what represents rule methods
     * @return list of rules meta-info
     */
    private static List<RuleInfo> getRulesDecorated(Method[] methods) {

        List<RuleInfo> rules = new ArrayList<RuleInfo>(methods.length);

        for (Method method : methods) {

            // Check that method should be ignored or not.
            if (isIgnored(method)) {
                continue;
            }

            String methodName = method.getName();

            Class<?>[] paramTypes = method.getParameterTypes();
            Class<?> returnType = VariationsResult.class;
            Class<?>[] newParams = new Class<?>[] { VariationsPack.class };
            Class<?>[] extendedParamTypes = (Class<?>[]) ArrayUtils.addAll(paramTypes, newParams);

            RuleInfo ruleInfoEnhanced = RulesFactory.createRuleInfo(methodName, extendedParamTypes, returnType);
            RuleInfo ruleInfoOriginal = RulesFactory.createRuleInfo(methodName, paramTypes, method.getReturnType());

            rules.add(ruleInfoEnhanced);
            rules.add(ruleInfoOriginal);
        }

        return rules;
    }

    /**
     * Searches for method that will be executed instead of method in enhanced
     * interface.
     * 
     * @param simpleClass Class without variations injection.
     * @param enhancedMethod Method enhanced with variations.
     * @return Corresponding method in original interface for method from
     *         enhanced interface.
     * @throws Exception Possible exception from java reflection caused wrong
     *             method accessing.
     */
    public static Method getMethodForEnhanced(Class<?> simpleClass, Method enhancedMethod) throws Exception {
        Class<?>[] parameterTypes = enhancedMethod.getParameterTypes();
        if (VariationsEnhancerHelper.isEnhancedMethod(enhancedMethod)) {
            return simpleClass.getMethod(enhancedMethod.getName(),
                Arrays.copyOf(parameterTypes, parameterTypes.length - 1));
        } else {
            return simpleClass.getMethod(enhancedMethod.getName(), parameterTypes);
        }
    }

    // FIXME skip enhanced methods
    /**
     * {@link ClassWriter} for creation undecorated class: 
     * all enhanced with variations methods will be removed from interface.
     * 
     * @author PUdalau
     */
    private static class UndecoratingClassWriter extends ClassAdapter {

        private static final String VARIATIONS_PACK_TYPE = "Lorg/openl/rules/project/instantiation/variation/VariationsPack;";
        private static final String VARIATIONS_RESULT_TYPE = "Lorg/openl/rules/project/instantiation/variation/VariationsResult;";
        private String className;

        public UndecoratingClassWriter(ClassVisitor delegatedClassVisitor, String className) {
            super(delegatedClassVisitor);
            this.className = className;
        }

        @Override
        public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) {
            super.visit(arg0, arg1, className.replace('.', '/'), arg3, arg4, arg5);
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String arg1, String arg2, String arg3, String[] arg4) {
            //write only unenhanced method
            if (!isEnhancedMethod(arg2)) {
                return super.visitMethod(arg0, arg1, arg2, arg3, arg4);
            } else {
                return null;//null means skip this method
            }
        }
        
        private boolean isEnhancedMethod(String signature){
            return signature.contains(VARIATIONS_PACK_TYPE)&& signature.endsWith(VARIATIONS_RESULT_TYPE);
        }

    }
}
