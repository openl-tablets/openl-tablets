package org.openl.rules.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.binding.impl.component.ComponentOpenClass.GetOpenClass;
import org.openl.binding.impl.component.ComponentOpenClass.ThisField;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ADynamicClass.OpenConstructor;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.util.ClassUtils;

/**
 * The factory class that provides methods to generate interface class using
 * methods (rules) of IOpenClass.
 * 
 */
public class InterfaceGenerator {

    public static final int PUBLIC_ABSTRACT_INTERFACE = Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT
            + Opcodes.ACC_INTERFACE;
    public static final int PUBLIC_ABSTRACT = Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT;
    public static final String JAVA_LANG_OBJECT = "java/lang/Object";
    private static final RuleInfo[] EMPTY_RULES = new RuleInfo[0];

    /**
     * Generates interface class using collection of rules.
     * 
     * @param className name of result class
     * @param rules collection of rules what will be used as interface methods
     * @param classLoader class loader what will be used to load generated
     *            interface
     * @return generated interface
     * @throws Exception if an error has occurred
     */
    public static Class<?> generateInterface(String className, RuleInfo[] rules, ClassLoader classLoader)
            throws Exception {

        ClassWriter classWriter = new ClassWriter(0);

        String name = className.replace('.', '/');

        classWriter.visit(Opcodes.V1_7, PUBLIC_ABSTRACT_INTERFACE, name, null, JAVA_LANG_OBJECT, null);

        for (RuleInfo ruleInfo : rules) {

            String ruleName = ruleInfo.getName();
            classWriter.visitMethod(PUBLIC_ABSTRACT, ruleName, getMethodTypes(ruleInfo), null, null);
        }

        classWriter.visitEnd();

        // Create class object.
        //
        ClassUtils.defineClass(className, classWriter.toByteArray(), classLoader);

        // Return loaded to classpath class object.
        //
        return Class.forName(className, true, classLoader);
    }
    
    /**
     * Generates interface class using methods and fields of given IOpenClass
     * instance.
     * 
     * @throws Exception if an error has occurred
     */
    public static Class<?> generateInterface(String className,
            IOpenClass openClass,
            ClassLoader classLoader,
            String[] includes,
            String[] excludes) throws Exception {

        if (openClass == null) {
            return generateInterface(className, EMPTY_RULES, classLoader);
        }

        List<RuleInfo> rules = new ArrayList<RuleInfo>();

        Set<MethodKey> methodsInClass = new HashSet<MethodKey>();
        
        final Collection<IOpenMethod> methods = openClass.getMethods();
        for (IOpenMethod method : methods) {
            if (!isIgnoredMember(method)) {
                RuleInfo ruleInfo = getRuleInfoForMethod(method);
                boolean isMember = isMember(ruleInfo, includes, excludes);
                if (isMember) {
                    rules.add(ruleInfo);
                    methodsInClass.add(new MethodKey(method));
                }
            }
        }

        final Collection<IOpenField> fields = openClass.getFields().values();
        for (IOpenField field : fields) {
            if (!isIgnoredMember(field)) {
                if (field.isReadable()) {
                    RuleInfo ruleInfo = getRuleInfoForField(field);
                    boolean isMember = isMember(ruleInfo, includes, excludes);
                    if (isMember) {
                        MethodKey key = new MethodKey(ruleInfo.getName(), IOpenClass.EMPTY, false);
                        //Skip getter for field if method is defined with the same signature.
                        if (!methodsInClass.contains(key)) {
                            rules.add(ruleInfo);
                            methodsInClass.add(key);
                        }
                    }
                }
            }
        }

        return generateInterface(className, rules.toArray(new RuleInfo[0]), classLoader);
    }

    private static boolean isMember(RuleInfo ruleInfo, String[] includes, String[] excludes) {
        boolean isMember = true;
        String methodSignature = getRuleInfoSignature(ruleInfo);
        if (includes != null && includes.length > 0) {
            isMember = false;
            for (String pattern : includes) {
                if (Pattern.matches(pattern, methodSignature)) {
                    isMember = true;
                }
            }
        }
        if (excludes != null && excludes.length > 0 && isMember) {
            for (String pattern : excludes) {
                if (Pattern.matches(pattern, methodSignature)) {
                    isMember = false;
                }
            }
        }
        return isMember;
    }

    private static String getRuleInfoSignature(RuleInfo ruleInfo){
        StringBuilder sb = new StringBuilder();
        sb.append(ruleInfo.getReturnType().getCanonicalName());
        sb.append(" ");
        sb.append(ruleInfo.getName());
        sb.append("(");
        boolean first = true;
        for (Class<?> paramType : ruleInfo.getParamTypes()){
            if (first){
                first = false;
            }else{
                sb.append(", ");
            }
            sb.append(paramType.getCanonicalName());
        }
        sb.append(")");
        return sb.toString();
    }
    
    /**
     * Generates interface class using methods and fields of given IOpenClass
     * instance.
     * 
     * @param className name of result class
     * @param openClass IOpenClass instance
     * @param classLoader class loader what will be used to load generated
     *            interface
     * @return generated interface
     * @throws Exception if an error has occurred
     */
    public static Class<?> generateInterface(String className, IOpenClass openClass, ClassLoader classLoader)
            throws Exception {
        return generateInterface(className, openClass, classLoader, null, null);
    }

    /**
     * Gets rule information of IOpenField instance.
     * 
     * @param field IOpenField instance
     * @return rule info
     */
    private static RuleInfo getRuleInfoForField(IOpenField field) {

        String methodName = ClassUtils.getter(field.getName());
        Class<?>[] paramTypes = new Class<?>[0];
        Class<?> returnType = field.getType().getInstanceClass();

        RuleInfo ruleInfo = createRuleInfo(methodName, paramTypes, returnType);

        return ruleInfo;
    }

    /**
     * Gets rule information of IOpenMethod instance.
     * 
     * @param method IOpenMethod instance
     * @return rule info
     */
    private static RuleInfo getRuleInfoForMethod(IOpenMethod method) {

        String methodName = method.getName();
        IOpenClass[] paramClasses = method.getSignature().getParameterTypes();
        Class<?> returnType = method.getType().getInstanceClass();

        Class<?>[] paramTypes = getInstanceClasses(paramClasses);

        RuleInfo ruleInfo = createRuleInfo(methodName, paramTypes, returnType);

        return ruleInfo;
    }

    /**
     * Creates rule info using rule name, parameters types and return type.
     * 
     * @param ruleName rule name
     * @param paramTypes parameters types
     * @param returnType return type
     * @return rule info
     */
    public static RuleInfo createRuleInfo(String ruleName, Class<?>[] paramTypes, Class<?> returnType) {

        RuleInfo ruleInfo = new RuleInfo();
        ruleInfo.setName(ruleName);
        ruleInfo.setParamTypes(paramTypes);
        ruleInfo.setReturnType(returnType);

        return ruleInfo;
    }

    /**
     * Checks that given member is ignored.
     * 
     * @param member member (method or field)
     * @return <code>true</code> - if member should be ignored (will be skipped
     *         due interface generation phase), <code>false</code> - otherwise
     */
    private static boolean isIgnoredMember(IOpenMember member) {
        return member instanceof OpenConstructor || member instanceof JavaOpenConstructor
                || member instanceof ThisField || member instanceof GetOpenClass || member instanceof TestSuiteMethod;
    }

    /**
     * Gets string that contains rule types (parameters types and return type).
     * 
     * @param ruleInfo rule info
     * @return string with rule types
     */
    private static String getMethodTypes(RuleInfo ruleInfo) {

        Class<?> returnType = ruleInfo.getReturnType();
        Class<?>[] paramTypes = ruleInfo.getParamTypes();

        StringBuilder builder = new StringBuilder("(");

        for (int i = 0; i < paramTypes.length; i++) {
            builder.append(Type.getType(paramTypes[i]));
        }

        builder.append(")");
        builder.append(Type.getType(returnType));

        return builder.toString();
    }

    /**
     * Convert open classes to array of instance classes.
     *
     * @param openClasses array of open classes
     * @return array of instance classes
     */
    private static Class<?>[] getInstanceClasses(IOpenClass[] openClasses) {

        List<Class<?>> classes = new ArrayList<Class<?>>();

        if (openClasses != null) {
            for (IOpenClass openClass : openClasses) {

                Class<?> clazz = openClass.getInstanceClass();
                classes.add(clazz);
            }
        }

        return classes.toArray(new Class<?>[classes.size()]);
    }
}
