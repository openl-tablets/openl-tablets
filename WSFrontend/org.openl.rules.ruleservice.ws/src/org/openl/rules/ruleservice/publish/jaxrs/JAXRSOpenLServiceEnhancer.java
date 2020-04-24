package org.openl.rules.ruleservice.publish.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.xml.ElementClass;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.rules.datatype.gen.ASMUtils;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.ruleservice.publish.common.MethodUtils;
import org.openl.runtime.ASMProxyFactory;
import org.openl.types.IOpenMethod;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.util.generation.InterfaceTransformer;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Utility class for generate JAXRS annotations for service interface.
 *
 * @author Marat Kamalov
 *
 */
public final class JAXRSOpenLServiceEnhancer {

    private static final String DEFAULT_VERSION = "1.0.0";

    private boolean resolveMethodParameterNamesEnabled = true;

    public boolean isResolveMethodParameterNamesEnabled() {
        return resolveMethodParameterNamesEnabled;
    }

    public void setResolveMethodParameterNamesEnabled(boolean resolveMethodParameterNamesEnabled) {
        this.resolveMethodParameterNamesEnabled = resolveMethodParameterNamesEnabled;
    }

    private static final Set<Class<?>> TEXT_MEDIA_TYPE_SET = new HashSet<>();
    static {
        TEXT_MEDIA_TYPE_SET.add(Number.class);
        TEXT_MEDIA_TYPE_SET.add(Enum.class);
        TEXT_MEDIA_TYPE_SET.add(String.class);
        TEXT_MEDIA_TYPE_SET.add(Date.class);
    }

    private static class ParamAnnotationValue {

        private final Class<?> annotationClass;
        private final String fieldName;

        public ParamAnnotationValue(Class<?> withPathParamValues, String fieldName) {
            this.annotationClass = withPathParamValues;
            this.fieldName = fieldName;
        }

        public Class<?> getAnnotationClass() {
            return annotationClass;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    private class JAXRSInterfaceAnnotationEnhancerClassVisitor extends ClassVisitor {

        private static final int MAX_PARAMETERS_COUNT_FOR_GET = 4;

        private static final String DECORATED_CLASS_NAME_SUFFIX = "$JAXRSAnnotated";

        private static final String REQUEST_PARAMETER_SUFFIX = "Request";

        private final Class<?> originalClass;
        private final OpenLService service;
        private final ClassLoader classLoader;
        private Map<Method, String> paths = null;
        private Map<Method, String> nicknames = null;
        private Map<Method, String> methodRequests = null;

        JAXRSInterfaceAnnotationEnhancerClassVisitor(ClassVisitor arg0,
                Class<?> originalClass,
                ClassLoader classLoader,
                OpenLService service) {
            super(Opcodes.ASM5, arg0);
            this.originalClass = originalClass;
            this.classLoader = classLoader;
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

            // Swagger annotation
            if (!originalClass.isAnnotationPresent(Api.class)) {
                this.visitAnnotation(Type.getDescriptor(Api.class), true);
            }
            if (!originalClass.isAnnotationPresent(SwaggerDefinition.class)) {
                AnnotationVisitor av = this.visitAnnotation(Type.getDescriptor(SwaggerDefinition.class), true);
                AnnotationVisitor av1 = av.visitAnnotation("info",
                    Type.getDescriptor(io.swagger.annotations.Info.class));
                av1.visit("title", service.getName());
                av1.visit("version", DEFAULT_VERSION);
                av1.visitEnd();
                av.visitEnd();
            }

            // OpenAPI annotation
            if (originalClass.getAnnotation(OpenAPIDefinition.class) == null) {
                AnnotationVisitor av = this.visitAnnotation(Type.getDescriptor(OpenAPIDefinition.class), true);
                AnnotationVisitor av1 = av.visitAnnotation("info", Type.getDescriptor(Info.class));
                av1.visit("title", service.getName());
                av1.visit("version", DEFAULT_VERSION);
                av1.visitEnd();
                av.visitEnd();
            }

            if (!originalClass.isAnnotationPresent(Path.class)) {
                AnnotationVisitor annotationVisitor = this.visitAnnotation(Type.getDescriptor(Path.class), true);
                annotationVisitor.visit("value", "/");
                annotationVisitor.visitEnd();
            }

            // Consumes annotation
            if (!originalClass.isAnnotationPresent(Consumes.class)) {
                addConsumesAnnotation(this);
            }
            // Produces annotation
            if (!originalClass.isAnnotationPresent(Produces.class)) {
                addProducesAnnotation(this);
            }
        }

        private String changeArgumentTypes(String signature, Method originalMethod) throws Exception {
            Class<?> argumentWrapperClass = generateWrapperClass(originalMethod);

            int index = signature.lastIndexOf(')');
            int indexb = signature.lastIndexOf('(');
            return signature.substring(0, indexb + 1) + Type.getDescriptor(argumentWrapperClass) + signature
                .substring(index);
        }

        private Class<?> generateWrapperClass(Method originalMethod) throws Exception {
            String[] parameterNames = MethodUtils.getParameterNames(originalMethod,
                JAXRSOpenLServiceEnhancer.this.isResolveMethodParameterNamesEnabled() ? service : null);
            String requestParameterName = getRequestParameterName(originalMethod);
            String beanName = "org.openl.jaxrs." + requestParameterName;

            int i = 0;
            WrapperBeanClassBuilder beanClassBuilder = new WrapperBeanClassBuilder(beanName, originalMethod.getName());
            for (Class<?> type : originalMethod.getParameterTypes()) {
                beanClassBuilder.addField(parameterNames[i], type.getName());
                i++;
            }

            byte[] byteCode = beanClassBuilder.byteCode();

            return ClassUtils.defineClass(beanName, byteCode, classLoader);
        }

        String getRequestParameterName(Method method) {
            if (methodRequests == null) {
                methodRequests = new HashMap<>();
                List<Method> methods = MethodUtils.sort(Arrays.asList(originalClass.getMethods()));

                Set<String> requestEntitiesCache = initRequestEntitiesCache(methods);
                for (Method m : methods) {
                    String name = StringUtils.capitalize(m.getName()) + REQUEST_PARAMETER_SUFFIX;
                    String s = name;
                    int i = 1;
                    while (requestEntitiesCache.contains(s)) {
                        s = name + i;
                        i++;
                    }
                    requestEntitiesCache.add(s);
                    methodRequests.put(m, s);
                }
            }

            return methodRequests.get(method);
        }

        private Set<String> initRequestEntitiesCache(List<Method> methods) {
            Set<String> cache = new HashSet<>();
            for (Method method : methods) {
                for (Class<?> paramType : method.getParameterTypes()) {
                    String requestEntityName = paramType.getSimpleName();
                    if (requestEntityName.contains(REQUEST_PARAMETER_SUFFIX)) {
                        cache.add(requestEntityName);
                    }
                }
            }
            return cache;
        }

        String getNickName(Method method) {
            if (nicknames == null) {
                nicknames = new HashMap<>();
                List<Method> methods = new ArrayList<>();
                List<Method> methods1 = new ArrayList<>();
                for (Method m : originalClass.getMethods()) {
                    if (m.isAnnotationPresent(Path.class)) {
                        methods1.add(m);
                    } else {
                        methods.add(m);
                    }
                }
                generateNickNames(methods);
                generateNickNames(methods1);
            }
            return nicknames.get(method);
        }

        private void generateNickNames(List<Method> methods) {
            methods = MethodUtils.sort(methods);
            for (Method m : methods) {
                String s = m.getName();
                int i = 1;
                while (nicknames.containsValue(s)) {
                    s = m.getName() + "_" + i;
                    i++;
                }
                nicknames.put(m, s);
            }
        }

        String getPath(Method method) {
            if (paths == null) {
                paths = new HashMap<>();
                List<Method> methods = new ArrayList<>();
                for (Method m : originalClass.getMethods()) {
                    Path pathAnnotation = m.getAnnotation(Path.class);
                    if (pathAnnotation != null) {
                        String value = pathAnnotation.value();

                        while (value.charAt(0) == '/') {
                            value = value.substring(1);
                        }

                        if (value.indexOf('/') > 0) {
                            value = value.substring(0, value.indexOf('/'));
                        }

                        paths.put(m, value);
                    } else {
                        methods.add(m);
                    }
                }

                methods = MethodUtils.sort(methods);

                for (Method m : methods) {
                    String s = m.getName();
                    int i = 1;
                    while (paths.containsValue(s)) {
                        s = m.getName() + i;
                        i++;
                    }
                    paths.put(m, s);
                }
            }

            return paths.get(method);
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String methodName, String arg2, String arg3, String[] arg4) {
            Method originalMethod = ASMUtils.getMethod(originalClass, methodName, arg2);
            if (originalMethod == null) {
                throw new RuleServiceRuntimeException("Method is not found in the original class.");
            }

            MethodVisitor mv;
            Class<?> returnType = originalMethod.getReturnType();
            boolean hasResponse = returnType.equals(Response.class);
            arg2 = hasResponse ? arg2
                               : arg2.substring(0, arg2.lastIndexOf(')') + 1) + Type.getDescriptor(Response.class);

            boolean allParametersIsPrimitive = true;
            Class<?>[] originalParameterTypes = originalMethod.getParameterTypes();
            int numOfParameters = originalParameterTypes.length;
            if (numOfParameters < MAX_PARAMETERS_COUNT_FOR_GET) {
                for (Class<?> parameterType : originalParameterTypes) {
                    if (!parameterType.isPrimitive()) {
                        allParametersIsPrimitive = false;
                        break;
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("/").append(getPath(originalMethod));
            if (numOfParameters < MAX_PARAMETERS_COUNT_FOR_GET && allParametersIsPrimitive && originalMethod
                .getAnnotation(POST.class) == null || originalMethod.getAnnotation(GET.class) != null) {
                mv = super.visitMethod(arg0, methodName, arg2, arg3, arg4);
                String[] parameterNames = MethodUtils.getParameterNames(originalMethod, service);
                processAnnotationsOnMethodParameters(originalMethod, mv);
                List<ParamAnnotationValue> paramAnnotationsValues = getParamAnnotationsValue(originalMethod);
                Set<String> usedValues = paramAnnotationsValues.stream()
                    .filter(Objects::nonNull)
                    .map(ParamAnnotationValue::getFieldName)
                    .collect(Collectors.toSet());
                int i = 0;
                for (String paramName : parameterNames) {
                    if (paramAnnotationsValues.get(i) == null) {
                        String p = paramName;
                        int j = 1;
                        while (usedValues.contains(p)) {
                            p = paramName + j;
                        }
                        sb.append("/{").append(p).append(": .*}");
                        addPathParamAnnotation(mv, i, p);
                        usedValues.add(p);
                    } else if (paramAnnotationsValues.get(i).getAnnotationClass().isAssignableFrom(PathParam.class)) {
                        sb.append("/{").append(paramAnnotationsValues.get(i).getFieldName()).append(": .*}");
                    }
                    i++;
                }

                addGetAnnotation(mv, originalMethod);
                addPathAnnotation(mv, originalMethod, sb.toString());
            } else {
                try {
                    if (numOfParameters > 1) {
                        String changeArgumentTypes = changeArgumentTypes(arg2, originalMethod);
                        mv = super.visitMethod(arg0, methodName, changeArgumentTypes, arg3, arg4);
                    } else {
                        mv = super.visitMethod(arg0, methodName, arg2, arg3, arg4);
                        processAnnotationsOnMethodParameters(originalMethod, mv);
                    }
                    if (!hasResponse) {
                        annotateReturnElementClass(mv, returnType);
                    }
                    if (originalMethod.getAnnotation(GET.class) == null) {
                        addPostAnnotation(mv, originalMethod);
                    }
                    addPathAnnotation(mv, originalMethod, sb.toString());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            addConsumerProducesMethodAnnotations(mv, returnType, originalParameterTypes, originalMethod);
            addSwaggerMethodAnnotation(mv, originalMethod, getNickName(originalMethod));
            return mv;
        }

        private List<ParamAnnotationValue> getParamAnnotationsValue(Method originalMethod) {
            final List<ParamAnnotationValue> values = new ArrayList<>(originalMethod.getParameterCount());
            for (Annotation[] annotations : originalMethod.getParameterAnnotations()) {
                if (annotations.length > 0) {
                    for (Annotation annotation : annotations) {
                        if (PathParam.class.equals(annotation.annotationType())) {
                            values.add(new ParamAnnotationValue(PathParam.class, ((PathParam) annotation).value()));
                            // it is possible that PathParam and QueryParam annotations will be indicated together for
                            // one parameter
                            break;
                        } else if (QueryParam.class.equals(annotation.annotationType())) {
                            values.add(new ParamAnnotationValue(QueryParam.class, ((QueryParam) annotation).value()));
                            // it is possible that PathParam and QueryParam annotations will be indicated together for
                            // one parameter
                            break;
                        }
                    }
                } else {
                    values.add(null);
                }
            }
            return values;
        }

        private boolean isTextMediaType(Class<?> type) {
            if (type.isPrimitive()) {
                return true;
            }
            for (Class<?> cl : TEXT_MEDIA_TYPE_SET) {
                if (cl.isAssignableFrom(type)) {
                    return true;
                }
            }
            return false;
        }

        private void addConsumerProducesMethodAnnotations(MethodVisitor mv,
                Class<?> returnType,
                Class<?>[] originalParameterTypes,
                Method originalMethod) {
            if (returnType != null && isTextMediaType(returnType) && originalMethod
                .getAnnotation(Produces.class) == null) {
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Produces.class), true);
                AnnotationVisitor av2 = av.visitArray("value");
                av2.visit(null, MediaType.TEXT_PLAIN);
                av2.visitEnd();
                av.visitEnd();
            }
            if (originalParameterTypes.length == 1 && isTextMediaType(originalParameterTypes[0]) && originalMethod
                .getAnnotation(Consumes.class) == null) {
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Consumes.class), true);
                AnnotationVisitor av2 = av.visitArray("value");
                av2.visit(null, MediaType.TEXT_PLAIN);
                av2.visitEnd();
                av.visitEnd();
            }
        }

        private void processAnnotationsOnMethodParameters(Method originalMethod, MethodVisitor mv) {
            int index = 0;
            for (Annotation[] annotations : originalMethod.getParameterAnnotations()) {
                for (Annotation annotation : annotations) {
                    AnnotationVisitor av = mv
                        .visitParameterAnnotation(index, Type.getDescriptor(annotation.annotationType()), true);
                    InterfaceTransformer.processAnnotation(annotation, av);
                }
                index++;
            }
        }

        private void annotateReturnElementClass(MethodVisitor mv, Class<?> returnType) {
            if (returnType.equals(Object.class) || returnType.equals(Void.class)) {
                return;
            }
            AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(ElementClass.class), true);
            av.visit("response", Type.getType(returnType));
            av.visitEnd();
        }

        private void addPostAnnotation(MethodVisitor mv, Method originalMethod) {
            if (!originalMethod.isAnnotationPresent(POST.class)) {
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(POST.class), true);
                av.visitEnd();
            }
        }

        private void addGetAnnotation(MethodVisitor mv, Method originalMethod) {
            if (!originalMethod.isAnnotationPresent(GET.class)) {
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(GET.class), true);
                av.visitEnd();
            }
        }

        private void addPathAnnotation(MethodVisitor mv, Method originalMethod, String path) {
            if (!originalMethod.isAnnotationPresent(Path.class)) {
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Path.class), true);
                av.visit("value", path);
                av.visitEnd();
            }
        }

        private void addSwaggerMethodAnnotation(MethodVisitor mv, Method originalMethod, String nickname) {
            if (!originalMethod.isAnnotationPresent(ApiOperation.class) || !originalMethod
                .isAnnotationPresent(Operation.class)) {
                String operationSummary;
                IOpenMethod openMethod = MethodUtils.getRulesMethod(originalMethod, service);
                if (openMethod != null) {
                    operationSummary = openMethod.getType().getDisplayName(INamedThing.SHORT) + " " + MethodUtil
                        .printSignature(openMethod, INamedThing.SHORT);
                } else {
                    operationSummary = originalMethod.getReturnType().getSimpleName() + " " + MethodUtil
                        .printMethod(originalMethod.getName(), originalMethod.getParameterTypes());
                }
                if (!originalMethod.isAnnotationPresent(ApiOperation.class)) {
                    AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(ApiOperation.class), true);
                    av.visit("value", operationSummary.substring(0, Math.min(operationSummary.length(), 120)));
                    av.visit("notes", (openMethod != null ? "Rules method: " : "Method: ") + operationSummary);
                    av.visit("response", Type.getType(originalMethod.getReturnType()));
                    av.visit("nickname", nickname);
                    av.visitEnd();
                }
                if (!originalMethod.isAnnotationPresent(Operation.class)) {
                    AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Operation.class), true);
                    av.visit("summary", operationSummary.substring(0, Math.min(operationSummary.length(), 120)));
                    av.visit("description", (openMethod != null ? "Rules method: " : "Method: ") + operationSummary);

                    if (!originalMethod.isAnnotationPresent(ApiResponse.class)) {
                        AnnotationVisitor av1 = av.visitArray("responses");
                        AnnotationVisitor av2 = av1.visitAnnotation("responses", Type.getDescriptor(ApiResponse.class));
                        AnnotationVisitor av3 = av2.visitArray("content");
                        AnnotationVisitor av4 = av3.visitAnnotation("responses", Type.getDescriptor(Content.class));
                        AnnotationVisitor av5 = av4.visitAnnotation("schema", Type.getDescriptor(Schema.class));
                        av5.visit("implementation", Type.getType(originalMethod.getReturnType()));
                        av5.visitEnd();
                        av4.visitEnd();
                        av3.visitEnd();
                        av2.visitEnd();
                        av1.visitEnd();
                    }

                    av.visitEnd();
                }
            }
        }

        private void addPathParamAnnotation(MethodVisitor mv, int index, String paramName) {
            AnnotationVisitor av = mv.visitParameterAnnotation(index, Type.getDescriptor(PathParam.class), true);
            av.visit("value", paramName);
            av.visitEnd();
        }

        private void addProducesAnnotation(ClassVisitor cv) {
            AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(Produces.class), true);
            AnnotationVisitor av1 = av.visitArray("value");
            av1.visit(null, MediaType.APPLICATION_JSON);
            av1.visitEnd();
            av.visitEnd();
        }

        private void addConsumesAnnotation(ClassVisitor cv) {
            AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(Consumes.class), true);
            AnnotationVisitor av1 = av.visitArray("value");
            av1.visit(null, MediaType.APPLICATION_JSON);
            av1.visitEnd();
            av.visitEnd();
        }
    }

    public Object decorateServiceBean(OpenLService service) throws Exception {
        Class<?> serviceClass = service.getServiceClass();
        Objects.requireNonNull(serviceClass, "Service class cannot be null");
        if (!serviceClass.isInterface()) {
            throw new IllegalStateException("Service class is not an interface.");
        }
        ClassLoader classLoader = service.getClassLoader();

        ClassWriter cw = new ClassWriter(0);
        JAXRSInterfaceAnnotationEnhancerClassVisitor jaxrsAnnotationEnhancerClassVisitor = new JAXRSInterfaceAnnotationEnhancerClassVisitor(
            cw,
            serviceClass,
            classLoader,
            service);
        String enhancedClassName = serviceClass
            .getName() + JAXRSInterfaceAnnotationEnhancerClassVisitor.DECORATED_CLASS_NAME_SUFFIX;
        InterfaceTransformer transformer = new InterfaceTransformer(serviceClass, enhancedClassName, false);
        transformer.accept(jaxrsAnnotationEnhancerClassVisitor);
        cw.visitEnd();

        Class<?> proxyInterface = ClassUtils.defineClass(enhancedClassName, cw.toByteArray(), classLoader);
        if (proxyInterface.getPackage() == null) {
            throw new IllegalStateException("Package cannot be null");
        }
        Map<Method, Method> methodMap = new HashMap<>();
        for (Method method : proxyInterface.getMethods()) {
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();

            try {
                Method targetMethod = serviceClass.getMethod(methodName, parameterTypes);
                methodMap.put(method, targetMethod);
            } catch (NoSuchMethodException ex) {
                if (parameterTypes.length == 1) {
                    Class<?> methodArgument = parameterTypes[0];
                    parameterTypes = (Class<?>[]) methodArgument.getMethod("_types").invoke(null);
                }
                Method targetMethod = serviceClass.getMethod(methodName, parameterTypes);
                methodMap.put(method, targetMethod);
            }
        }
        return ASMProxyFactory
            .newProxyInstance(classLoader, new JAXRSMethodHandler(service.getServiceBean(), methodMap), proxyInterface);
    }
}
