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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.binding.MethodUtil;
import org.openl.rules.datatype.gen.ASMUtils;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.ruleservice.publish.common.MethodUtils;
import org.openl.runtime.ASMProxyFactory;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;

/**
 * Utility class for generate JAXWS annotations for service interface.
 *
 * @author Marat Kamalov
 *
 */
public final class JAXWSOpenLServiceEnhancer {

    private boolean resolveMethodParameterNamesEnabled = true;

    public boolean isResolveMethodParameterNamesEnabled() {
        return resolveMethodParameterNamesEnabled;
    }

    public void setResolveMethodParameterNamesEnabled(boolean resolveMethodParameterNamesEnabled) {
        this.resolveMethodParameterNamesEnabled = resolveMethodParameterNamesEnabled;
    }

    private class JAXWSInterfaceAnnotationEnhancerClassVisitor extends ClassVisitor {
        private static final String DECORATED_CLASS_NAME_SUFFIX = "$JAXWSAnnotated";

        private Class<?> originalClass;
        private OpenLService service;

        private Map<Method, String> operationNames = null;

        JAXWSInterfaceAnnotationEnhancerClassVisitor(ClassVisitor arg0, Class<?> originalClass, OpenLService service) {
            super(Opcodes.ASM5, arg0);
            this.originalClass = originalClass;
            this.service = service;
        }

        @Override
        public void visit(int version,
                int access,
                String name,
                String signature,
                String superName,
                String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            boolean requiredWebServiceAnnotation = !originalClass.isAnnotationPresent(WebService.class);
            if (requiredWebServiceAnnotation) {
                AnnotationVisitor annotationVisitor = this.visitAnnotation(Type.getDescriptor(WebService.class), true);
                if (service != null) {
                    try {
                        if (service.getServiceClassName() != null) {
                            annotationVisitor.visit("serviceName", originalClass.getSimpleName());
                            annotationVisitor.visit("name", originalClass.getSimpleName() + "PortType");
                            annotationVisitor.visit("portName", originalClass.getSimpleName() + "PortType");
                        } else {
                            annotationVisitor.visit("serviceName", service.getName());
                            annotationVisitor.visit("name", service.getName() + "PortType");
                            annotationVisitor.visit("portName", service.getName() + "PortType");
                            annotationVisitor.visit("targetNamespace", "http://DefaultNamespace");
                        }
                    } catch (RuleServiceInstantiationException e) {
                        // Skip
                    }
                }
                annotationVisitor.visitEnd();
            }
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String methodName, String arg2, String arg3, String[] arg4) {
            Method originalMethod = ASMUtils.getMethod(originalClass, methodName, arg2);
            if (originalMethod == null) {
                throw new RuleServiceRuntimeException("Method is not found in the original class");
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
            try {
                if (service != null && service.getServiceClassName() == null && JAXWSOpenLServiceEnhancer.this
                    .isResolveMethodParameterNamesEnabled()) { // Set
                    // parameter
                    // names
                    // only
                    // for
                    // generated
                    // interfaces
                    String[] parameterNames = MethodUtils.getParameterNames(originalMethod, service);
                    int i = 0;
                    for (String paramName : parameterNames) {
                        Annotation[] annotations = originalMethod.getParameterAnnotations()[i];
                        boolean found = false;
                        for (Annotation ann : annotations) {
                            if (ann instanceof WebParam) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            addWebParamAnnotation(mv, i, paramName);
                        }
                        i++;
                    }
                }
            } catch (RuleServiceInstantiationException ignore) {
                // Skip
            }
            return mv;
        }

        private String getOperationName(Method method) {
            if (operationNames == null) {
                operationNames = new HashMap<>();
                Set<String> operations = new HashSet<>();
                List<Method> methods = new ArrayList<>();

                for (Method m : originalClass.getMethods()) {
                    Annotation webMethod = m.getAnnotation(WebMethod.class);
                    if (webMethod != null) {
                        operations.add(((WebMethod) webMethod).operationName());
                    } else {
                        methods.add(m);
                    }
                }

                methods = MethodUtils.sort(methods);

                for (Method m : methods) {
                    String s = m.getName();
                    int i = 0;
                    while (operations.contains(s)) {
                        i++;
                        s = m.getName() + i;
                    }
                    operations.add(s);
                    operationNames.put(m, s);
                }
            }
            return operationNames.get(method);
        }

        private void addWebParamAnnotation(MethodVisitor mv, int index, String paramName) {
            AnnotationVisitor av = mv.visitParameterAnnotation(index, Type.getDescriptor(WebParam.class), true);
            av.visit("name", paramName);
            av.visitEnd();
        }
    }

    private ClassLoader getClassLoader(OpenLService service) throws RuleServiceInstantiationException {
        ClassLoader classLoader = null;
        if (service != null) {
            classLoader = service.getClassLoader();
        }
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        return classLoader;
    }

    public Class<?> decorateServiceInterface(OpenLService service) throws Exception {
        if (service.getServiceClass() == null) {
            throw new IllegalStateException("Service class is null.");
        }
        if (!service.getServiceClass().isInterface()) {
            throw new IllegalStateException("Service class is not an interface.");
        }
        String enhancedClassName = service.getServiceClass()
            .getName() + JAXWSInterfaceAnnotationEnhancerClassVisitor.DECORATED_CLASS_NAME_SUFFIX;

        ClassWriter cw = new ClassWriter(0);
        JAXWSInterfaceAnnotationEnhancerClassVisitor jaxrsAnnotationEnhancerClassVisitor = this.new JAXWSInterfaceAnnotationEnhancerClassVisitor(
            cw,
            service.getServiceClass(),
            service);
        InterfaceTransformer transformer = new InterfaceTransformer(service.getServiceClass(), enhancedClassName);
        transformer.accept(jaxrsAnnotationEnhancerClassVisitor);
        cw.visitEnd();

        ClassLoader classLoader = getClassLoader(service);

        return ClassUtils.defineClass(enhancedClassName, cw.toByteArray(), classLoader);
    }

    public Object createServiceProxy(Class<?> proxyInterface,
            Class<?> serviceClass,
            OpenLService service) throws RulesInstantiationException, RuleServiceInstantiationException {
        Map<Method, Method> methodMap = new HashMap<>();
        for (Method method : proxyInterface.getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            try {
                Method targetMethod = serviceClass.getMethod(method.getName(), parameterTypes);
                methodMap.put(targetMethod, method);
            } catch (NoSuchMethodException ex) {
                throw new RulesInstantiationException(String.format(
                    "Failed to find corresponding method in original class for method '%s' in service '%s'",
                    MethodUtil.printMethod(method.getName(), method.getParameterTypes()),
                    service.getName()));
            }
        }
        return ASMProxyFactory.newProxyInstance(service.getClassLoader(),
            new JAXWSMethodHandler(service.getServiceBean(), methodMap),
            serviceClass);
    }

}
