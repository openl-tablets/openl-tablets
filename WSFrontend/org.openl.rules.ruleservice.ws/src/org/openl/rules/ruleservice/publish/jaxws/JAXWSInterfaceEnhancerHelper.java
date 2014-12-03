package org.openl.rules.ruleservice.publish.jaxws;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import net.sf.cglib.core.ReflectUtils;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.publish.common.MethodSorter;
import org.openl.rules.variation.VariationsPack;
import org.openl.types.IOpenMethod;
import org.openl.util.generation.InterfaceTransformer;

/**
 * Utility class for generate JAXWS annotations for service interface.
 * 
 * @author Marat Kamalov
 *
 */
public class JAXWSInterfaceEnhancerHelper {

    private static class JAXWSInterfaceAnnotationEnhancerClassVisitor extends ClassVisitor {
        private static final String DECORATED_CLASS_NAME_SUFFIX = "$JAXWSAnnotated";

        private Class<?> originalClass;
        private OpenLService service;

        private Map<Method, String> operationNames = null;

        public JAXWSInterfaceAnnotationEnhancerClassVisitor(ClassVisitor arg0,
                Class<?> originalClass,
                OpenLService service) {
            super(Opcodes.ASM4, arg0);
            this.originalClass = originalClass;
            this.service = service;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            boolean requiredWebServiceAnnotation = true;
            for (Annotation annotation : originalClass.getAnnotations()) {
                if (annotation.annotationType().equals(WebService.class)) {
                    requiredWebServiceAnnotation = false;
                    break;
                }
            }
            if (requiredWebServiceAnnotation) {
                AnnotationVisitor annotationVisitor = this.visitAnnotation(Type.getDescriptor(WebService.class), true);
                if (service.getServiceClassName() != null) {
                    annotationVisitor.visit("serviceName", service.getServiceClassName());
                } else {
                    annotationVisitor.visit("serviceName", service.getName());
                    annotationVisitor.visit("targetNamespace", "http://DefaultNamespace");
                }
                annotationVisitor.visitEnd();
            }
        }

        private String[] getParameterNames(Method method) {
            if (service != null && service.getOpenClass() != null) {
                for (IOpenMethod m : service.getOpenClass().getMethods()) {
                    if (m.getName().equals(method.getName())) {
                        int i = 0;
                        boolean f = true;
                        boolean skipRuntimeContextParameter = false;
                        boolean variationPackIsLastParameter = false;
                        for (Class<?> clazz : method.getParameterTypes()) {
                            if (service.isProvideRuntimeContext() && !skipRuntimeContextParameter) {
                                skipRuntimeContextParameter = true;
                                continue;
                            }
                            if (i == method.getParameterTypes().length - 1 && service.isProvideVariations() && clazz.isAssignableFrom(VariationsPack.class)) {
                                variationPackIsLastParameter = true;
                                continue;
                            }
                            if (i >= m.getSignature().getNumberOfParameters()) {
                                f = false;
                                break;
                            }
                            if (!clazz.equals(m.getSignature().getParameterType(i).getInstanceClass())) {
                                f = false;
                                break;
                            }
                            i++;
                        }
                        if (f) {
                            List<String> parameterNames = new ArrayList<String>();
                            if (service.isProvideRuntimeContext()) {
                                parameterNames.add("runtimeContext");
                            }
                            for (i = 0; i < m.getSignature().getNumberOfParameters(); i++) {
                                parameterNames.add(m.getSignature().getParameterName(i));
                            }
                            if (variationPackIsLastParameter) {
                                parameterNames.add("variationPack");
                            }
                            return parameterNames.toArray(new String[] {});
                        }
                    }
                }
            }
            String[] parameterNames = new String[method.getParameterTypes().length];
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                parameterNames[i] = "arg" + i;
            }
            return parameterNames;
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String methodName, String arg2, String arg3, String[] arg4) {
            Method originalMethod = findOriginalMethod(methodName, arg2);
            if (originalMethod == null) {
                throw new OpenLRuntimeException("Method not found in original class!");
            }

            MethodVisitor mv = super.visitMethod(arg0, methodName, arg2, arg3, arg4);

            boolean foundWebMethodAnnotation = false;
            if (originalMethod.getAnnotation(WebMethod.class) != null) {
                foundWebMethodAnnotation = true;
            }

            if (!foundWebMethodAnnotation) { // Add WebMethod annotation
                String operationName = getOperationName(originalMethod);
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(WebMethod.class), true);
                av.visit("operationName", operationName);
                av.visitEnd();
            }

            String[] parameterNames = getParameterNames(originalMethod);
            int i = 0;
            for (String paramName : parameterNames) {
                Annotation[] annotations = originalMethod.getParameterAnnotations()[i];
                boolean found = false;
                for (Annotation ann : annotations) {
                    if (ann.annotationType().equals(WebParam.class)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    addWebParamAnnotation(mv, i, paramName);
                }
                i++;
            }

            return mv;
        }

        private String getOperationName(Method method) {
            if (operationNames == null) {
                operationNames = new HashMap<Method, String>();
                Set<String> operations = new HashSet<String>();
                List<Method> methods = new ArrayList<Method>();

                for (Method m : originalClass.getMethods()) {
                    Annotation webMethod = m.getAnnotation(WebMethod.class);
                    if (webMethod != null) {
                        operations.add(((WebMethod) webMethod).operationName());
                    } else {
                        methods.add(m);
                    }
                }

                methods = MethodSorter.sort(methods);

                for (Method m : methods) {
                    String s = m.getName();
                    int i = 0;
                    while (operations.contains(s)) {
                        i++;
                        s = m.getName() + String.valueOf(i);
                    }
                    operations.add(s);
                    operationNames.put(m, s);
                }
            }
            return operationNames.get(method);
        }

        private Method findOriginalMethod(String methodName, String argumentTypes) {
            Method originalMethod = null;
            for (Method method : originalClass.getMethods()) {
                if (originalMethod == null && methodName.equals(method.getName())) {
                    Type[] typesInOriginalClassMethod = Type.getArgumentTypes(method);
                    Type[] typesInCurrentMethod = Type.getArgumentTypes(argumentTypes);
                    if (typesInCurrentMethod.length == typesInOriginalClassMethod.length) {
                        boolean f = true;
                        for (int i = 0; i < typesInCurrentMethod.length; i++) {
                            if (!typesInCurrentMethod[i].equals(typesInOriginalClassMethod[i])) {
                                f = false;
                            }
                        }
                        if (f) {
                            originalMethod = method;
                        }
                    }
                }
            }
            return originalMethod;
        }

        private void addWebParamAnnotation(MethodVisitor mv, int index, String paramName) {
            AnnotationVisitor av = mv.visitParameterAnnotation(index, Type.getDescriptor(WebParam.class), true);
            av.visit("name", paramName);
            av.visitEnd();
        }
    }

    public static Class<?> decorateInterface(Class<?> originalClass, ClassLoader classLoader, OpenLService service) throws Exception {
        if (originalClass == null) {
            throw new IllegalArgumentException("Original class is mandatory argument!");
        }
        if (!originalClass.isInterface()) {
            throw new IllegalArgumentException("Original class should be an interface!");
        }
        ClassWriter cw = new ClassWriter(0);
        JAXWSInterfaceAnnotationEnhancerClassVisitor jaxrsAnnotationEnhancerClassVisitor = new JAXWSInterfaceAnnotationEnhancerClassVisitor(cw,
            originalClass,
            service);
        String enchancedClassName = originalClass.getCanonicalName() + JAXWSInterfaceAnnotationEnhancerClassVisitor.DECORATED_CLASS_NAME_SUFFIX;
        InterfaceTransformer transformer = new InterfaceTransformer(originalClass, enchancedClassName);
        transformer.accept(jaxrsAnnotationEnhancerClassVisitor);
        cw.visitEnd();
        Class<?> enchancedClass = ReflectUtils.defineClass(enchancedClassName, cw.toByteArray(), classLoader);
        return enchancedClass;
    }

    public static Class<?> decorateInterface(Class<?> originalClass, OpenLService service) throws Exception {
        return decorateInterface(originalClass, Thread.currentThread().getContextClassLoader(), service);
    }
}
