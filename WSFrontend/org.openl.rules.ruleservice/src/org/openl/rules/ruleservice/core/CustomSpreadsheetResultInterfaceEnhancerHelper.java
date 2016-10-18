package org.openl.rules.ruleservice.core;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.cglib.core.ReflectUtils;
import org.objectweb.asm.*;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.util.generation.InterfaceTransformer;

public class CustomSpreadsheetResultInterfaceEnhancerHelper {

    /**
     * Special ClassAdapter to generate interface with {@link Object} as the
     * return type for methods that have custom spreadsheet result.
     * 
     * @author Marat Kamalov
     */
    private static class CustomSpreadsheetResultInterfaceClassVisitor extends ClassVisitor {

        private static final String DECORATED_CLASS_NAME_SUFFIX = "$Original";

        private Collection<Method> methods;

        /**
         * Constructs instanse with delegated {@link ClassVisitor} and set of
         * methods.
         * 
         * @param visitor delegated {@link ClassVisitor}.
         * @param methods Methods where to change return type.
         */
        public CustomSpreadsheetResultInterfaceClassVisitor(ClassVisitor visitor, Collection<Method> methods) {
	    super(Opcodes.ASM4, visitor);
            this.methods = methods;
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String arg1, String arg2, String arg3, String[] arg4) {
            boolean contains = false;
            for (Method method : methods) {
                if (arg1.equals(method.getName()) && arg2.equals(Type.getMethodDescriptor(method))) {
                    contains = true;
                    break;
                }
            }

            if (contains) {
                return super.visitMethod(arg0, arg1, convertReturnType(arg2), arg3, arg4);
            } else {
                return super.visitMethod(arg0, arg1, arg2, arg3, arg4);
            }
        }

        private String convertReturnType(String arg2) {
            int index = arg2.lastIndexOf(')');
            return arg2.substring(0, index + 1) + Type.getDescriptor(SpreadsheetResult.class);
        }
    }

    public static Class<?> decorate(Class<?> originalClass, ClassLoader classLoader) throws Exception {
        ClassWriter cw = new ClassWriter(0);

        Set<Method> methodsWithCustomSpreadSheetResult = new HashSet<Method>();

        for (Method method : originalClass.getMethods()) {
            Class<?> returnType = method.getReturnType();
            boolean f = false;
            if (SpreadsheetResult.class.isAssignableFrom(returnType) && !SpreadsheetResult.class.equals(returnType)) {
                if (returnType.getCanonicalName().equals(Spreadsheet.CUSTOMSPREADSHEETRESULT_TYPE_PREFIX + method.getName())){
                    for (Method m : returnType.getDeclaredMethods()) {
                        if (m.getName().startsWith("get$")) {
                            f = true;
                            break;
                        }
                    }
                }
            }
            if (f) {
                methodsWithCustomSpreadSheetResult.add(method);
            }
        }
        
        if (methodsWithCustomSpreadSheetResult.isEmpty()){
            return originalClass;
        }

        CustomSpreadsheetResultInterfaceClassVisitor customSpreadsheetResultInterfaceClassVisitor = new CustomSpreadsheetResultInterfaceClassVisitor(cw,
            methodsWithCustomSpreadSheetResult);

        String enchancedClassName = originalClass.getCanonicalName() + CustomSpreadsheetResultInterfaceClassVisitor.DECORATED_CLASS_NAME_SUFFIX;
        InterfaceTransformer transformer = new InterfaceTransformer(originalClass, enchancedClassName);
        transformer.accept(customSpreadsheetResultInterfaceClassVisitor);
        cw.visitEnd();
        Class<?> enchancedClass = ReflectUtils.defineClass(enchancedClassName, cw.toByteArray(), classLoader);
        return enchancedClass;
    }

    public static Class<?> decorate(Class<?> originalClass) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return decorate(originalClass, classLoader);
    }
}
