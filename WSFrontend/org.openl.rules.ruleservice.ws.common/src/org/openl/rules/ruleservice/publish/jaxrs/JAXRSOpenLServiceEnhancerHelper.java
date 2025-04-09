package org.openl.rules.ruleservice.publish.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.ext.xml.ElementClass;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.classloader.ClassLoaderUtils;
import org.openl.gen.FieldDescription;
import org.openl.rules.datatype.gen.ASMUtils;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationHelper;
import org.openl.rules.ruleservice.core.annotations.ApiErrors;
import org.openl.rules.ruleservice.publish.common.MethodUtils;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.util.JAXBUtils;
import org.openl.util.StringUtils;
import org.openl.util.generation.InterfaceTransformer;

public class JAXRSOpenLServiceEnhancerHelper {

    public static final int MAX_PARAMETERS_COUNT_FOR_GET = 3;
    public static final int UNPROCESSABLE_ENTITY = 422;
    private static final String DECORATED_CLASS_NAME_SUFFIX = "$JAXRSAnnotated";

    private static final Set<Class<?>> TEXT_MEDIA_TYPE_SET = new HashSet<>();

    static {
        TEXT_MEDIA_TYPE_SET.add(Number.class);
        TEXT_MEDIA_TYPE_SET.add(Enum.class);
        TEXT_MEDIA_TYPE_SET.add(String.class);
        TEXT_MEDIA_TYPE_SET.add(Date.class);
        TEXT_MEDIA_TYPE_SET.add(Instant.class);
        TEXT_MEDIA_TYPE_SET.add(ZonedDateTime.class);
        TEXT_MEDIA_TYPE_SET.add(LocalDateTime.class);
        TEXT_MEDIA_TYPE_SET.add(LocalDate.class);
        TEXT_MEDIA_TYPE_SET.add(LocalTime.class);
        TEXT_MEDIA_TYPE_SET.add(Character.class);
        TEXT_MEDIA_TYPE_SET.add(Boolean.class);
    }

    private static class JAXRSInterfaceAnnotationEnhancerClassVisitor extends ClassVisitor {
        private static final String DEFAULT_VERSION = "1.0.0";

        private static final String UNPROCESSABLE_ENTITY_MESSAGE = "Custom user errors in rules or validation errors in input parameters";
        private static final String UNPROCESSABLE_ENTITY_EXAMPLE = "{\"message\": \"Some message\", \"type\": \"USER_ERROR\"}";
        private static final String USER_ERROR_EXAMPLE = "{\"message\": \"Some message\", \"code\": \"code.example\", \"type\": \"USER_ERROR\"}";
        private static final String BAD_REQUEST_MESSAGE = "Invalid request format e.g. missing required field, unparseable JSON value, etc.";
        private static final String BAD_REQUEST_EXAMPLE = "{\"message\": \"Cannot parse 'bar' to JSON\", \"type\": \"BAD_REQUEST\"}";
        private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server errors e.g. compilation or parsing errors, runtime exceptions, etc.";
        private static final String INTERNAL_SERVER_ERROR_EXAMPLE = "{\"message\": \"Failed to load lazy method.\", \"type\": \"COMPILATION\"}";
        private static final Class<?>[] DEFAULT_API_ERROR_TYPES = new Class<?>[]{JAXRSErrorResponse.class};

        private static final String REQUEST_PARAMETER_SUFFIX = "Request";

        private final Class<?> originalClass;
        private final ClassLoader classLoader;
        private Set<String> usedPaths = null;
        private final Set<String> nicknames = new HashSet<>();
        private final boolean provideRuntimeContext;
        private final boolean provideVariations;
        private Set<String> usedOpenApiComponentNamesWithRequestParameterSuffix = null;
        private final Object targetService;

        private final Map<String, List<Method>> originalClassMethodsByName;

        JAXRSInterfaceAnnotationEnhancerClassVisitor(ClassVisitor arg0,
                                                     Class<?> originalClass,
                                                     Object targetService,
                                                     ClassLoader classLoader,
                                                     boolean provideRuntimeContext,
                                                     boolean provideVariations) {
            super(Opcodes.ASM5, arg0);
            this.originalClass = Objects.requireNonNull(originalClass, "originalClass cannot be null");
            this.classLoader = classLoader;
            this.provideRuntimeContext = provideRuntimeContext;
            this.provideVariations = provideVariations;
            this.targetService = targetService;

            this.originalClassMethodsByName = ASMUtils.buildMap(originalClass);
        }

        @Override
        public void visit(int version,
                          int access,
                          String name,
                          String signature,
                          String superName,
                          String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);

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
            // Error responses annotation
            if (!originalClass
                    .isAnnotationPresent(ApiResponses.class) && !originalClass
                    .isAnnotationPresent(ApiResponse.class)) {
                addOpenApiResponsesAnnotation(this);
            }
        }

        private String getOpenApiComponentName(Class<?> clazz) {
            return Optional.ofNullable(clazz.getAnnotation(Schema.class))
                    .map(Schema::name)
                    .map(StringUtils::trimToNull)
                    .orElseGet(clazz::getSimpleName);
        }

        private Set<String> getUsedOpenApiComponentNamesWithRequestParameterSuffix() {
            if (usedOpenApiComponentNamesWithRequestParameterSuffix == null) {
                usedOpenApiComponentNamesWithRequestParameterSuffix = new HashSet<>();
                for (Method method : originalClass.getDeclaredMethods()) {
                    processClassForOpenApiComponentNamesConflictResolving(method.getReturnType());
                    for (Class<?> paramType : method.getParameterTypes()) {
                        processClassForOpenApiComponentNamesConflictResolving(paramType);
                    }
                }
            }
            return usedOpenApiComponentNamesWithRequestParameterSuffix;
        }

        private void processClassForOpenApiComponentNamesConflictResolving(Class<?> type) {
            while (type.isArray()) {
                type = type.getComponentType();
            }
            String componentName = getOpenApiComponentName(type);
            if (isConflictPossible(componentName)) {
                usedOpenApiComponentNamesWithRequestParameterSuffix.add(componentName);
            }
        }

        private boolean isConflictPossible(String name) {
            while (Character.isDigit(name.charAt(name.length() - 1))) {
                name = name.substring(0, name.length() - 1);
            }
            return name.endsWith(REQUEST_PARAMETER_SUFFIX);
        }

        private String changedParameterTypesDescription(String descriptor,
                                                        Method originalMethod,
                                                        int suffix) throws Exception {
            Class<?> parameterWrapperClass = generateWrapperClass(originalMethod, suffix);
            List<Type> types = new ArrayList<>();
            types.add(Type.getType(parameterWrapperClass));
            for (Parameter parameter : originalMethod.getParameters()) {
                if (!isParameterInWrapperClass(parameter)) {
                    types.add(Type.getType(parameter.getType()));
                }
            }
            return Type.getMethodDescriptor(Type.getReturnType(descriptor), types.toArray(new Type[0]));
        }

        private Class<?> generateWrapperClass(Method originalMethod, int suffix) throws Exception {
            String[] parameterNames = resolveParameterNames(originalMethod);
            String requestParameterName = StringUtils.capitalize(originalMethod.getName()) + REQUEST_PARAMETER_SUFFIX;
            if (suffix > 0) {
                requestParameterName = requestParameterName + suffix;
            }
            String nonConflictedRequestParameterName = requestParameterName;
            StringBuilder s = new StringBuilder("0");
            while (getUsedOpenApiComponentNamesWithRequestParameterSuffix()
                    .contains(nonConflictedRequestParameterName)) {
                nonConflictedRequestParameterName = StringUtils
                        .capitalize(originalMethod.getName()) + REQUEST_PARAMETER_SUFFIX + s + (suffix > 0 ? suffix : "");
                s.insert(0, "0");
            }
            usedOpenApiComponentNamesWithRequestParameterSuffix.add(nonConflictedRequestParameterName);
            String beanName = "org.openl.jaxrs." + nonConflictedRequestParameterName;

            int i = 0;
            WrapperBeanClassBuilder beanClassBuilder = new WrapperBeanClassBuilder(beanName, originalMethod.getName());
            LinkedHashMap<String, FieldDescription> originalMethodTypeFields = new LinkedHashMap<>();
            for (Parameter parameter : originalMethod.getParameters()) {
                if (isParameterInWrapperClass(parameter)) {
                    beanClassBuilder.addField(parameterNames[i], parameter.getType().getName());
                }
                originalMethodTypeFields.put(parameterNames[i], new FieldDescription(parameter.getType().getName()));
                i++;
            }
            beanClassBuilder.setOriginalMethodTypeFields(originalMethodTypeFields);
            byte[] byteCode = beanClassBuilder.byteCode();

            return ClassLoaderUtils.defineClass(beanName, byteCode, classLoader);
        }

        Set<String> getUsedPaths() {
            if (usedPaths == null) {
                usedPaths = new HashSet<>();
                for (Method m : originalClass.getMethods()) {
                    Path pathAnnotation = m.getAnnotation(Path.class);
                    if (pathAnnotation != null) {
                        String value = pathAnnotation.value();
                        usedPaths.add(normalizePath(value));
                    }
                }
            }
            return usedPaths;
        }

        String normalizePath(String path) {
            while (path.charAt(0) == '/') {
                path = path.substring(1);
            }
            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return path.replaceAll("\\{[^}]*}", "{}");
        }

        boolean isJAXRSParamAnnotation(Parameter parameter) {
            return parameter.isAnnotationPresent(Multipart.class) || parameter
                    .isAnnotationPresent(PathParam.class) || parameter.isAnnotationPresent(QueryParam.class) || parameter
                    .isAnnotationPresent(CookieParam.class) || parameter.isAnnotationPresent(
                    FormParam.class) || parameter.isAnnotationPresent(BeanParam.class) || parameter
                    .isAnnotationPresent(HeaderParam.class) || parameter.isAnnotationPresent(MatrixParam.class);
        }

        boolean isJAXRSParamAnnotationUsedInMethod(Method method) {
            for (Parameter parameter : method.getParameters()) {
                if (isJAXRSParamAnnotation(parameter)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public MethodVisitor visitMethod(final int access,
                                         final String name,
                                         String descriptor,
                                         final String signature,
                                         final String[] exceptions) {
            Method originalMethod = ASMUtils.findMethod(originalClassMethodsByName, name, descriptor);
            if (originalMethod == null) {
                throw new IllegalStateException("Method is not found in the original class");
            }

            MethodVisitor mv;
            Class<?> returnType = extractOriginalType(originalMethod.getReturnType());
            boolean hasResponse = returnType == Response.class;
            descriptor = hasResponse ? descriptor
                    : Type.getMethodDescriptor(Type.getType(Response.class),
                    Type.getArgumentTypes(descriptor));

            boolean allParametersIsPrimitive = true;
            Class<?>[] originalParameterTypes = originalMethod.getParameterTypes();
            int numOfParameters = originalParameterTypes.length;
            for (Parameter parameter : originalMethod.getParameters()) {
                if (!isParameterInWrapperClass(parameter)) {
                    numOfParameters--;
                }
            }
            if (numOfParameters <= MAX_PARAMETERS_COUNT_FOR_GET) {
                for (Class<?> parameterType : originalParameterTypes) {
                    if (!parameterType.isPrimitive()) {
                        allParametersIsPrimitive = false;
                        break;
                    }
                }
            }
            if (numOfParameters <= MAX_PARAMETERS_COUNT_FOR_GET && allParametersIsPrimitive && httpAnnotationIsNotPresented(
                    originalMethod) || originalMethod.isAnnotationPresent(GET.class)) {
                StringBuilder sb = new StringBuilder();
                mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                String[] parameterNames = resolveParameterNames(originalMethod);
                processAnnotationsOnMethodParameters(originalMethod, mv);
                addGetAnnotation(mv, originalMethod);
                if (!originalMethod.isAnnotationPresent(Path.class)) {
                    Set<String> usedPathParamValues = getUsedValuesInParamAnnotations(originalMethod,
                            PathParam.class,
                            PathParam::value);
                    int i = 0;
                    for (String paramName : parameterNames) {
                        Parameter parameter = originalMethod.getParameters()[i];
                        PathParam pathParam = parameter.getAnnotation(PathParam.class);
                        if (pathParam == null) {
                            String p = paramName;
                            int j = 1;
                            while (usedPathParamValues.contains(p)) {
                                p = paramName + j;
                            }
                            sb.append("/{").append(p).append(": .*}");
                            addPathParamAnnotation(mv, i, p);
                            usedPathParamValues.add(p);
                        } else {
                            sb.append("/{").append(pathParam.value()).append(": .*}");
                        }
                        i++;
                    }
                    if (!originalMethod.isAnnotationPresent(Path.class)) {
                        String path = "/" + originalMethod.getName() + sb;
                        int c = 1;
                        while (getUsedPaths().contains(normalizePath(path))) {
                            path = "/" + originalMethod.getName() + (c++) + sb;
                        }
                        getUsedPaths().add(normalizePath(path));
                        addPathAnnotation(mv, originalMethod, path);
                    }
                } else {
                    Set<String> usedQueryParamValues = getUsedValuesInParamAnnotations(originalMethod,
                            QueryParam.class,
                            QueryParam::value);
                    int i = 0;
                    for (String paramName : parameterNames) {
                        boolean jaxrsAnnotationPresented = isJAXRSParamAnnotation(originalMethod.getParameters()[i]);
                        if (!jaxrsAnnotationPresented) {
                            String p = paramName;
                            int j = 1;
                            while (usedQueryParamValues.contains(p)) {
                                p = paramName + j;
                            }
                            addQueryParamAnnotation(mv, i, p);
                            usedQueryParamValues.add(p);
                        }
                        i++;
                    }
                }
            } else {
                try {
                    String path = null;
                    int c = 0;
                    if (!originalMethod.isAnnotationPresent(Path.class)) {
                        path = "/" + originalMethod.getName();
                        while (getUsedPaths().contains(normalizePath(path))) {
                            c++;
                            path = "/" + originalMethod.getName() + c;
                        }
                        getUsedPaths().add(normalizePath(path));
                    }
                    if (numOfParameters > 1) {
                        if (!isJAXRSParamAnnotationUsedInMethod(originalMethod)) {
                            mv = super.visitMethod(access,
                                    name,
                                    changedParameterTypesDescription(descriptor, originalMethod, c),
                                    signature,
                                    exceptions);
                            processAnnotationsOnMethodExternalParameters(originalMethod, mv);
                        } else {
                            mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                            processAnnotationsOnMethodParameters(originalMethod, mv);
                        }
                    } else {
                        mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                        processAnnotationsOnMethodParameters(originalMethod, mv);
                    }
                    if (!hasResponse) {
                        annotateReturnElementClass(mv, returnType);
                    }
                    if (httpAnnotationIsNotPresented(originalMethod)) {
                        addPostAnnotation(mv, originalMethod);
                    }
                    addPathAnnotation(mv, originalMethod, path);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            addConsumerProducesMethodAnnotations(mv, returnType, originalParameterTypes, originalMethod);
            String nickname = originalMethod.getName();
            int c = 1;
            while (nicknames.contains(nickname)) {
                nickname = originalMethod.getName() + "_" + c++;
            }
            nicknames.add(nickname);
            addSwaggerMethodAnnotation(mv, originalMethod, nickname);
            addOpenApiResponsesMethodAnnotation(mv, originalMethod);
            addOpenApiAcceptLanguageHeader(mv, originalMethod);
            return mv;
        }

        private boolean httpAnnotationIsNotPresented(Method originalMethod) {
            return !(originalMethod.isAnnotationPresent(POST.class) || originalMethod
                    .isAnnotationPresent(PATCH.class) || originalMethod.isAnnotationPresent(DELETE.class) || originalMethod
                    .isAnnotationPresent(PUT.class) || originalMethod
                    .isAnnotationPresent(OPTIONS.class) || originalMethod.isAnnotationPresent(HEAD.class));
        }

        private <T extends Annotation> Set<String> getUsedValuesInParamAnnotations(Method originalMethod,
                                                                                   Class<T> annotationClass,
                                                                                   Function<T, String> func) {
            Set<String> usedPathParamValues = new HashSet<>();
            for (Parameter parameter : originalMethod.getParameters()) {
                T annotation = parameter.getAnnotation(annotationClass);
                if (annotation != null) {
                    usedPathParamValues.add(func.apply(annotation));
                    break;
                }
            }
            return usedPathParamValues;
        }

        private String[] resolveParameterNames(Method originalMethod) {
            IOpenMember openMember = RuleServiceOpenLServiceInstantiationHelper.getOpenMember(originalMethod,
                    targetService);
            return MethodUtils.getParameterNames(openMember, originalMethod, provideRuntimeContext, provideVariations);
        }

        private boolean isTextMediaType(Class<?> type) {
            if (type == void.class || type == Void.class) {
                return false;
            }
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
            if (returnType != null && isTextMediaType(returnType) && !originalMethod
                    .isAnnotationPresent(Produces.class)) {
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Produces.class), true);
                AnnotationVisitor av2 = av.visitArray("value");
                av2.visit(null, "text/plain;charset=UTF-8"); // All I/O of Strings are serialized as UTF-8
                av2.visitEnd();
                av.visitEnd();
            }
            if (originalParameterTypes.length == 1 && isTextMediaType(originalParameterTypes[0]) && !originalMethod
                    .isAnnotationPresent(Consumes.class)) {
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Consumes.class), true);
                AnnotationVisitor av2 = av.visitArray("value");
                av2.visit(null, MediaType.TEXT_PLAIN);
                av2.visitEnd();
                av.visitEnd();
            }
        }

        private void processAnnotationsOnMethodExternalParameters(Method originalMethod, MethodVisitor mv) {
            int index = 1; // Skip first wrapper class parameter
            for (Parameter parameter : originalMethod.getParameters()) {
                if (!isParameterInWrapperClass(parameter)) {
                    for (Annotation annotation : parameter.getAnnotations()) {
                        AnnotationVisitor av = mv
                                .visitParameterAnnotation(index, Type.getDescriptor(annotation.annotationType()), true);
                        InterfaceTransformer.processAnnotation(annotation, av);
                    }
                    index++;
                }
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
            if (Object.class == returnType || Void.class == returnType) {
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
            if (!originalMethod.isAnnotationPresent(Operation.class)) {
                String summary = originalMethod.getReturnType().getSimpleName() + " " + MethodUtil
                        .printMethod(originalMethod.getName(), originalMethod.getParameterTypes(), true);
                IOpenMember openMember = RuleServiceOpenLServiceInstantiationHelper.getOpenMember(originalMethod,
                        targetService);
                IOpenMethod openMethod = openMember instanceof IOpenMethod ? (IOpenMethod) openMember : null;
                String detailedSummary = openMethod != null ? openMethod.getType()
                        .getDisplayName(INamedThing.LONG) + " " + MethodUtil.printSignature(openMethod, INamedThing.LONG)
                        : originalMethod.getReturnType()
                        .getTypeName() + " " + MethodUtil.printMethod(
                        originalMethod.getName(),
                        originalMethod.getParameterTypes(),
                        false);
                String truncatedSummary = summary.substring(0, Math.min(summary.length(), 120));
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Operation.class), true);
                av.visit("operationId", nickname);
                av.visit("summary", truncatedSummary);
                av.visit("description", (openMethod != null ? "Rules method: " : "Method: ") + detailedSummary);
                av.visitEnd();
            }
        }

        private void addOpenApiResponsesMethodAnnotation(MethodVisitor mv, Method originalMethod) {
            if (!isApiResponsesSpecified(originalMethod)) {
                Class<?> type = extractOriginalType(originalMethod.getReturnType());
                final boolean isVoidType = void.class == type || Void.class == type;
                AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(ApiResponses.class), true);
                Class<?> t = originalMethod.getReturnType();
                int dim = 0;
                while (t.isArray()) {
                    t = t.getComponentType();
                    dim++;
                }
                AnnotationVisitor av1 = av.visitArray("value");
                if (isVoidType || !type.isPrimitive()) {
                    // empty response body can be only for void or non-primitive types
                    AnnotationVisitor noContentAv = av1.visitAnnotation(null,
                            Type.getDescriptor(ApiResponse.class));
                    noContentAv.visit("responseCode", String.valueOf(Response.Status.NO_CONTENT.getStatusCode()));
                    noContentAv.visit("description", "Successful operation");
                    noContentAv.visitEnd();
                }
                if (!isVoidType) {
                    AnnotationVisitor av2 = av1.visitAnnotation("responses",
                            Type.getDescriptor(ApiResponse.class));
                    av2.visit("responseCode", String.valueOf(Response.Status.OK.getStatusCode()));
                    av2.visit("description", "Successful operation");
                    AnnotationVisitor av3 = av2.visitArray("content");
                    AnnotationVisitor av4 = av3.visitAnnotation("responses", Type.getDescriptor(Content.class));
                    if (dim < 2) {
                        addSchemaOpenApiAnnotation(av4, originalMethod.getReturnType());
                    } else {
                        addSchemaOpenApiAnnotation(av4, Object.class);
                    }
                    av4.visitEnd();
                    av3.visitEnd();
                    av2.visitEnd();
                }
                av1.visitEnd();

                av.visitEnd();
            }
        }

        private void addOpenApiAcceptLanguageHeader(MethodVisitor mv, Method originalMethod) {
            if (!originalMethod.isAnnotationPresent(Parameters.class) && !originalMethod
                    .isAnnotationPresent(io.swagger.v3.oas.annotations.Parameter.class)) {
                // empty response body can be only for void or non-primitive types
                var acceptLanguage = mv.visitAnnotation("Lio/swagger/v3/oas/annotations/Parameter;", true);
                acceptLanguage.visit("name", "Accept-Language");
                acceptLanguage.visitEnum("in", "Lio/swagger/v3/oas/annotations/enums/ParameterIn;", "HEADER");
                acceptLanguage.visit("example", "en-GB");
                var av1 = acceptLanguage.visitAnnotation("schema", "Lio/swagger/v3/oas/annotations/media/Schema;");
                av1.visit("name", "string");
                av1.visitEnd();
                acceptLanguage.visitEnd();
            }
        }

        private boolean isApiResponsesSpecified(Method originalMethod) {
            if (originalMethod.isAnnotationPresent(ApiResponses.class) || originalMethod.isAnnotationPresent(ApiResponse.class)) {
                return true;
            }
            if (originalMethod.isAnnotationPresent(Operation.class)) {
                Operation operationAnnotation = originalMethod.getAnnotation(Operation.class);
                return operationAnnotation.responses().length != 0;
            }
            return false;
        }

        private void addSchemaOpenApiAnnotation(AnnotationVisitor av, Class<?> type) {
            boolean isArrayOrCollection = type.isArray() || Collection.class.isAssignableFrom(type);
            if (isArrayOrCollection) {
                av = av.visitAnnotation("array", Type.getDescriptor(ArraySchema.class));
                type = Collection.class.isAssignableFrom(type) ? Object.class : type.getComponentType();
            }
            final Class<?> extractedType = extractOriginalType(type);
            if (extractedType != null) {
                type = extractedType;
            }
            AnnotationVisitor av1 = av.visitAnnotation("schema", Type.getDescriptor(Schema.class));
            if (type == Integer.class || type == int.class || type == Short.class || type == short.class || type == Byte.class || type == byte.class) {
                av1.visit("type", "integer");
                av1.visit("format", "int32");
            } else if (type == Long.class || type == long.class) {
                av1.visit("type", "integer");
                av1.visit("format", "int64");
            } else if (type == Float.class || type == float.class) {
                av1.visit("type", "number");
                av1.visit("format", "float");
            } else if (type == Double.class || type == double.class) {
                av1.visit("type", "number");
                av1.visit("format", "double");
            } else if (type == Boolean.class || type == boolean.class) {
                av1.visit("type", "boolean");
            } else if (type == Character.class || type == char.class) {
                av1.visit("type", "string");
            } else if (Map.class.isAssignableFrom(type)) {
                av1.visit("implementation", Type.getType(Object.class)); // Impossible to define Map through Schema
                // annotations.
            } else {
                av1.visit("implementation", Type.getType(type));
            }
            av1.visitEnd();
            if (isArrayOrCollection) {
                av.visitEnd();
            }
        }

        private void addPathParamAnnotation(MethodVisitor mv, int index, String paramName) {
            AnnotationVisitor av = mv.visitParameterAnnotation(index, Type.getDescriptor(PathParam.class), true);
            av.visit("value", paramName);
            av.visitEnd();
        }

        private void addQueryParamAnnotation(MethodVisitor mv, int index, String paramName) {
            AnnotationVisitor av = mv.visitParameterAnnotation(index, Type.getDescriptor(QueryParam.class), true);
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

        private void addOpenApiResponsesAnnotation(ClassVisitor cv) {
            AnnotationVisitor av = cv
                    .visitAnnotation(Type.getDescriptor(ApiResponses.class), true);
            AnnotationVisitor arrayAv = av.visitArray("value");

            List<Class<?>> allUserApiResponses = new ArrayList<>();
            allUserApiResponses.add(JAXRSUserErrorResponse.class);
            allUserApiResponses.addAll(Arrays.asList(DEFAULT_API_ERROR_TYPES));
            if (originalClass.isAnnotationPresent(ApiErrors.class)) {
                ApiErrors apiErrors = originalClass.getDeclaredAnnotation(ApiErrors.class);
                if (apiErrors.value() != null) {
                    allUserApiResponses.addAll(Arrays.asList(apiErrors.value()));
                }
            }

            addOpenApiResponseAnnotation(arrayAv,
                    UNPROCESSABLE_ENTITY,
                    UNPROCESSABLE_ENTITY_MESSAGE,
                    allUserApiResponses.toArray(new Class<?>[0]),
                    UNPROCESSABLE_ENTITY_EXAMPLE,
                    USER_ERROR_EXAMPLE);
            addOpenApiResponseAnnotation(arrayAv,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    BAD_REQUEST_MESSAGE,
                    DEFAULT_API_ERROR_TYPES,
                    BAD_REQUEST_EXAMPLE);
            addOpenApiResponseAnnotation(arrayAv,
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    INTERNAL_SERVER_ERROR_MESSAGE,
                    DEFAULT_API_ERROR_TYPES,
                    INTERNAL_SERVER_ERROR_EXAMPLE);

            arrayAv.visitEnd();
            av.visitEnd();
        }

        private void addOpenApiResponseAnnotation(AnnotationVisitor av,
                                                  int code,
                                                  String message,
                                                  Class<?>[] responseTypes,
                                                  String... jsonExamples) {
            AnnotationVisitor apiResponseAv = av.visitAnnotation(null,
                    Type.getDescriptor(ApiResponse.class));
            apiResponseAv.visit("responseCode", String.valueOf(code));
            apiResponseAv.visit("description", message);

            AnnotationVisitor contentArrayAv = apiResponseAv.visitArray("content");
            AnnotationVisitor contentAv = contentArrayAv.visitAnnotation(null, Type.getDescriptor(Content.class));
            contentAv.visit("mediaType", MediaType.APPLICATION_JSON);

            AnnotationVisitor schemaAv = contentAv.visitAnnotation("schema", Type.getDescriptor(Schema.class));
            if (responseTypes.length == 1) {
                schemaAv.visit("implementation", Type.getType(responseTypes[0]));
            } else {
                AnnotationVisitor oneOf = schemaAv.visitArray("oneOf");
                for (Class<?> respType : responseTypes) {
                    oneOf.visit(null, Type.getType(respType));
                }
                oneOf.visitEnd();
            }
            schemaAv.visitEnd();

            AnnotationVisitor examplesArrAv = contentAv.visitArray("examples");
            int exampleCnt = 1;
            for (String jsonExample : jsonExamples) {
                AnnotationVisitor exampleObjectAv = examplesArrAv.visitAnnotation(null,
                        Type.getDescriptor(ExampleObject.class));
                exampleObjectAv.visit("value", jsonExample);
                if (jsonExamples.length > 1) {
                    // if more than one example then add name
                    exampleObjectAv.visit("name", "Example " + exampleCnt++);
                }
                exampleObjectAv.visitEnd();
            }
            examplesArrAv.visitEnd();

            contentAv.visitEnd();
            contentArrayAv.visitEnd();
            apiResponseAv.visitEnd();
        }

        private Class<?> extractOriginalType(Class<?> type) {
            Class<?> extractedType = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(type);
            return extractedType == null ? type : extractedType;
        }

    }

    public static Map<Method, Method> buildMethodMap(Class<?> serviceClass,
                                                     Class<?> enhancedServiceClass) throws Exception {
        Map<Method, Method> methodMap = new HashMap<>();
        for (Method method : enhancedServiceClass.getMethods()) {
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            try {
                Method targetMethod = serviceClass.getMethod(methodName, parameterTypes);
                methodMap.put(method, targetMethod);
            } catch (NoSuchMethodException ex) {
                if (parameterTypes.length > 0) {
                    Class<?> methodArgument = parameterTypes[0];
                    parameterTypes = (Class<?>[]) methodArgument.getMethod("_types").invoke(null);
                }
                Method targetMethod = serviceClass.getMethod(methodName, parameterTypes);
                methodMap.put(method, targetMethod);
            }
        }
        return methodMap;
    }

    public static boolean isParameterInWrapperClass(Parameter parameter) {
        return !parameter.isAnnotationPresent(Context.class);
    }

    public static Class<?> enhanceInterface(Class<?> originalClass,
                                            Object targetService,
                                            ClassLoader classLoader,
                                            boolean provideRuntimeContext,
                                            boolean provideVariations) throws Exception {
        if (!originalClass.isInterface()) {
            throw new IllegalArgumentException("Only interfaces are supported");
        }
        final String enhancedClassName = originalClass.getName() + DECORATED_CLASS_NAME_SUFFIX;
        Class<?> enhancedClass;
        try {
            enhancedClass = classLoader.loadClass(enhancedClassName);
        } catch (ClassNotFoundException e) {
            ClassWriter cw = new ClassWriter(0);
            JAXRSInterfaceAnnotationEnhancerClassVisitor enhancerClassVisitor = new JAXRSInterfaceAnnotationEnhancerClassVisitor(
                    cw,
                    originalClass,
                    targetService,
                    classLoader,
                    provideRuntimeContext,
                    provideVariations
            );
            InterfaceTransformer transformer = new InterfaceTransformer(originalClass,
                    enhancedClassName,
                    InterfaceTransformer.IGNORE_PARAMETER_ANNOTATIONS);
            transformer.accept(enhancerClassVisitor);
            cw.visitEnd();
            enhancedClass = ClassLoaderUtils.defineClass(enhancedClassName, cw.toByteArray(), classLoader);
        }
        return enhancedClass;
    }
}
