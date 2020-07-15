package org.open.rules.project.validation.openapi;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.jxpath.CompiledExpression;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;
import org.openl.CompiledOpenClass;
import org.openl.base.INamedThing;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.resolving.ProjectResource;
import org.openl.rules.project.resolving.ProjectResourceLoader;
import org.openl.rules.project.validation.AbstractServiceInterfaceProjectValidator;
import org.openl.rules.project.validation.base.ValidatedCompiledOpenClass;
import org.openl.rules.ruleservice.publish.common.MethodUtils;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSOpenLServiceEnhancerHelper;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.OpenApiObjectMapperHack;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.OpenApiRulesCacheWorkaround;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.OpenApiSupportConverter;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson.OpenApiObjectMapperConfigurationHelper;
import org.openl.rules.serialization.JacksonObjectMapperFactoryBean;
import org.openl.rules.variation.VariationsPack;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.util.RefUtils;

public class OpenApiProjectValidator extends AbstractServiceInterfaceProjectValidator {

    static {
        ModelConverters.getInstance().addConverter(new OpenApiSupportConverter());
    }

    private static final String OPENAPI_JSON = "openapi.json";
    private static final String OPENAPI_YAML = "openapi.yaml";
    private static final String OPENAPI_YML = "openapi.yml";

    private boolean resolveMethodParameterNames = true;

    private final Reader reader = new Reader();

    private OpenAPI loadOpenAPI(ProjectDescriptor projectDescriptor, CompiledOpenClass compiledOpenClass) {
        ProjectResourceLoader projectResourceLoader = new ProjectResourceLoader(compiledOpenClass);
        ProjectResource projectResource = loadProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_JSON);
        if (projectResource == null) {
            projectResource = loadProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_YAML);
        }
        if (projectResource == null) {
            projectResource = loadProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_YML);
        }
        if (projectResource != null) {
            OpenAPIParser openApiParser = new OpenAPIParser();
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            options.setFlatten(true);
            try {
                String content = new String(Files.readAllBytes(Paths.get(projectResource.getFile())));
                return openApiParser.readContents(content, null, options).getOpenAPI();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public CompiledOpenClass validate(ProjectDescriptor projectDescriptor,
            RulesInstantiationStrategy rulesInstantiationStrategy) throws RulesInstantiationException {
        final CompiledOpenClass compiledOpenClass = rulesInstantiationStrategy.compile();
        OpenAPI expectedOpenAPI = loadOpenAPI(projectDescriptor, compiledOpenClass);
        if (expectedOpenAPI == null) {
            return compiledOpenClass;
        }
        final ValidatedCompiledOpenClass validatedCompiledOpenClass = new OpenApiValidatedCompiledOpenClass(
            compiledOpenClass);
        final Context context = new Context();
        context.setExpectedOpenAPI(expectedOpenAPI);
        context.setExpectedOpenAPIJXPathContext(JXPathContext.newContext(expectedOpenAPI));
        context.setValidatedCompiledOpenClass(validatedCompiledOpenClass);
        context.setOpenClass(validatedCompiledOpenClass.getOpenClassWithErrors());
        Class<?> serviceClass = resolveInterface(projectDescriptor,
            rulesInstantiationStrategy,
            validatedCompiledOpenClass);
        Class<?> enhancedServiceClass;
        try {
            enhancedServiceClass = enhanceWithJAXRS(context,
                projectDescriptor,
                validatedCompiledOpenClass,
                serviceClass);
        } catch (Exception e) {
            validatedCompiledOpenClass.addValidationMessage(
                OpenLMessagesUtils.newErrorMessage("Failed to build the interface for the project."));
            return validatedCompiledOpenClass;
        }
        context.setServiceClass(enhancedServiceClass);
        try {
            context.setMethodMap(JAXRSOpenLServiceEnhancerHelper.buildMethodMap(serviceClass, enhancedServiceClass));
        } catch (Exception e) {
            validatedCompiledOpenClass.addValidationMessage(
                OpenLMessagesUtils.newErrorMessage("Failed to build the interface for the project."));
            return validatedCompiledOpenClass;
        }
        OpenAPI actualOpenAPI;
        RulesDeploy rulesDeploy = getRulesDeploy(projectDescriptor, compiledOpenClass);
        context.setRulesDeploy(rulesDeploy);
        ObjectMapper objectMapper = createObjectMapper(context);
        synchronized (OpenApiRulesCacheWorkaround.class) {
            OpenApiRulesCacheWorkaround.reset();
            OpenApiObjectMapperHack openApiObjectMapperHack = new OpenApiObjectMapperHack();
            openApiObjectMapperHack.apply(objectMapper);
            actualOpenAPI = reader.read(enhancedServiceClass);
            openApiObjectMapperHack.revert();
        }
        context.setActualOpenAPI(actualOpenAPI);
        context.setActualOpenAPIJXPathContext(JXPathContext.newContext(actualOpenAPI));
        validateOpenAPI(context);
        return validatedCompiledOpenClass;
    }

    private ObjectMapper createObjectMapper(Context context) {
        ClassLoader classLoader = context.getValidatedCompiledOpenClass().getClassLoader();
        JacksonObjectMapperFactoryBean jacksonObjectMapperFactoryBean = new JacksonObjectMapperFactoryBean();
        jacksonObjectMapperFactoryBean.setClassLoader(classLoader);
        Set<Class<?>> rootClassNamesBindingClasses = getRootClassNamesBindingClasses(context.getRulesDeploy(),
            classLoader);
        jacksonObjectMapperFactoryBean.setOverrideClasses(rootClassNamesBindingClasses);
        jacksonObjectMapperFactoryBean.setSupportVariations(context.isProvideVariations());
        try {
            ObjectMapper objectMapper = jacksonObjectMapperFactoryBean.createJacksonObjectMapper();
            return OpenApiObjectMapperConfigurationHelper.configure(objectMapper);
        } catch (ClassNotFoundException ignore) { // Never happens
            return Json.mapper();
        }
    }

    private Set<Class<?>> getRootClassNamesBindingClasses(RulesDeploy rulesDeploy, ClassLoader classLoader) {
        if (rulesDeploy != null && rulesDeploy.getConfiguration() != null) {
            Object rootClassNamesBinding = rulesDeploy.getConfiguration().get("rootClassNamesBinding");
            Set<Class<?>> rootClassNamesBindingClasses = new HashSet<>();
            if (rootClassNamesBinding instanceof String) {
                String[] rootClasses = ((String) rootClassNamesBinding).split(",");
                for (String className : rootClasses) {
                    if (className != null && className.trim().length() > 0) {
                        try {
                            Class<?> cls = classLoader.loadClass(className);
                            rootClassNamesBindingClasses.add(cls);
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                }
            }
            return rootClassNamesBindingClasses;
        }
        return Collections.emptySet();
    }

    private Class<?> enhanceWithJAXRS(Context context,
            ProjectDescriptor projectDescriptor,
            CompiledOpenClass compiledOpenClass,
            Class<?> originalClass) throws Exception {
        RulesDeploy rulesDeploy = getRulesDeploy(projectDescriptor, compiledOpenClass);
        final boolean provideRuntimeContext = rulesDeploy == null && isProvideRuntimeContext() || rulesDeploy != null && Boolean.TRUE
            .equals(rulesDeploy.isProvideRuntimeContext());
        final boolean provideVariations = rulesDeploy == null && isProvideVariations() || rulesDeploy != null && Boolean.TRUE
            .equals(rulesDeploy.isProvideVariations());
        context.setProvideRuntimeContext(provideRuntimeContext);
        context.setProvideVariations(provideVariations);
        return JAXRSOpenLServiceEnhancerHelper.enhanceInterface(originalClass,
            compiledOpenClass.getOpenClassWithErrors(),
            originalClass.getClassLoader(),
            "unknown",
            "unknown",
            isResolveMethodParameterNames(),
            provideRuntimeContext,
            provideVariations);
    }

    private void validateOpenAPI(Context context) {
        if (context.getExpectedOpenAPI().getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : context.getExpectedOpenAPI().getPaths().entrySet()) {
                PathItem expectedPathItem = entry.getValue();
                try {
                    if (context.getActualOpenAPI().getPaths() != null) {
                        PathItem actualPathItem = context.getActualOpenAPI().getPaths().get(entry.getKey());
                        if (actualPathItem != null) {
                            context.setPath(entry.getKey());
                            context.setExpectedPathItem(expectedPathItem);
                            context.setActualPathItem(actualPathItem);
                            Method method = findMethodByPath(context.getServiceClass(), entry.getKey());
                            IOpenMethod openMethod = MethodUtils.findRulesMethod(context.getOpenClass(),
                                context.getMethodMap().get(method),
                                context.isProvideRuntimeContext(),
                                context.isProvideVariations());
                            if (openMethod == null) {
                                // Skip extra methods
                                continue;
                            }
                            context.setMethod(method);
                            context.setOpenMethod(openMethod);
                            validatePathItem(context);
                        } else {
                            addMethodError(context,
                                String.format(
                                    "The method related to the path item '%s' is found, but it is specified in the OpenAPI file.",
                                    context.getPath()));
                        }
                    }
                } finally {
                    context.setOpenMethod(null);
                    context.setActualPathItem(null);
                    context.setExpectedPathItem(null);
                    context.setPath(null);
                    context.setMethod(null);
                }
            }
        }
        if (context.getActualOpenAPI().getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : context.getActualOpenAPI().getPaths().entrySet()) {
                PathItem expectedPathItem = context.getExpectedOpenAPI().getPaths().get(entry.getKey());
                if (expectedPathItem == null) {
                    addMethodError(context,
                        String.format(
                            "Unexpected method related to the path item '%s' is found, but it is not specified in the OpenAPI file.",
                            entry.getKey()));
                }
            }
        }
    }

    private void addMethodWarning(Context context, String summary) {
        // TODO Remove duplicates, add error only for the current project
        if (context.getTableSyntaxNode() != null) {
            OpenLMessage openLMessage = OpenLMessagesUtils.newWarnMessage(summary, context.getTableSyntaxNode());
            context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
        } else {
            addWarning(context, summary);
        }
    }

    private void addMethodError(Context context, String summary) {
        // TODO Remove duplicates, add error only for the current project
        if (context.getTableSyntaxNode() != null) {
            SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(summary,
                context.getTableSyntaxNode());
            OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
            context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
            context.getTableSyntaxNode().addError(syntaxNodeException);
        } else {
            addError(context, summary);
        }
    }

    private void addError(Context context, String summary) {
        context.getValidatedCompiledOpenClass().addValidationMessage(OpenLMessagesUtils.newErrorMessage(summary));
    }

    private void addWarning(Context context, String summary) {
        context.getValidatedCompiledOpenClass().addValidationMessage(OpenLMessagesUtils.newWarnMessage(summary));
    }

    private void addTypeError(Context context, String summary) {
        // TODO Remove duplicates, add error only for the current project
        if (context.getType() instanceof DatatypeOpenClass) {
            DatatypeOpenClass datatypeOpenClass = (DatatypeOpenClass) context.getType();
            if (datatypeOpenClass.getTableSyntaxNode() != null) {
                SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(summary,
                    datatypeOpenClass.getTableSyntaxNode());
                OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
                context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
                datatypeOpenClass.getTableSyntaxNode().addError(syntaxNodeException);
                return;
            }
        }
        addError(context, summary);
    }

    private void addTypeWarning(Context context, IOpenClass openClass, String summary) {
        // TODO Remove duplicates, add error only for the current project
        if (openClass instanceof DatatypeOpenClass) {
            DatatypeOpenClass datatypeOpenClass = (DatatypeOpenClass) openClass;
            if (datatypeOpenClass.getTableSyntaxNode() != null) {
                OpenLMessage openLMessage = OpenLMessagesUtils.newWarnMessage(summary,
                    datatypeOpenClass.getTableSyntaxNode());
                context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
                return;
            }
        }
        context.getValidatedCompiledOpenClass().addValidationMessage(OpenLMessagesUtils.newWarnMessage(summary));
    }

    private void getAndValidateOperation(Context context, Function<PathItem, Operation> func, String operationType) {
        PathItem actualPathItem = context.getActualPathItem();
        PathItem expectedPathItem = context.getExpectedPathItem();

        Operation expectedOperation = func.apply(expectedPathItem);
        Operation actualOperation = func.apply(actualPathItem);

        if (expectedOperation != null || actualOperation != null) {
            if (actualOperation == null) {
                addMethodError(context,
                    String.format(
                        "The operation '%s' related to the path item '%s' is found, but is not specified in the OpenAPI file.",
                        operationType,
                        context.getPath()));
            } else if (expectedOperation == null) {
                addMethodError(context,
                    String.format(
                        "The operation '%s' related to the path item '%s' is not found, but it is specified in the OpenAPI file.",
                        operationType,
                        context.getPath()));
            } else {
                try {
                    context.setExpectedOperation(expectedOperation);
                    context.setActualOperation(actualOperation);
                    context.setOperationType(operationType);
                    validateOperation(context);
                } finally {
                    context.setExpectedOperation(null);
                    context.setActualOperation(null);
                    context.setOperationType(null);
                }
            }
        }
    }

    private Method findMethodByPath(Class<?> serviceClass, String path) {
        for (Method method : serviceClass.getMethods()) {
            Path pathAnnotation = method.getAnnotation(Path.class);
            if (pathAnnotation != null && Objects.equals(pathAnnotation.value(), path)) {
                return method;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T resolve(Context context, JXPathContext jxPathContext, T obj, Function<T, String> getRefFunc) {
        if (obj != null && getRefFunc.apply(obj) != null) {
            return resolve(context,
                jxPathContext,
                (T) resolveByRef(context, jxPathContext, getRefFunc.apply(obj)),
                getRefFunc);
        }
        return obj;
    }

    private Object resolveByRef(Context context, JXPathContext jxPathContext, String ref) {
        CompiledExpression compiledExpression = JXPathContext.compile(ref.substring(1));
        try {
            return compiledExpression.createPath(jxPathContext).getValue();
        } catch (JXPathException e) {
            if (context.isTypeValidationInProgress()) {
                addTypeError(context, String.format("Invalid $ref '%s' is found in the OpenAPI file.", ref));
            } else {
                addMethodError(context, String.format("Invalid $ref '%s' is found in the OpenAPI file.", ref));
            }
            return null;
        }
    }

    private void validateOperation(Context context) {
        Operation expectedOperation = context.getExpectedOperation();
        Operation actualOperation = context.getActualOperation();
        RequestBody actualRequestBody = resolve(context,
            context.getActualOpenAPIJXPathContext(),
            actualOperation.getRequestBody(),
            RequestBody::get$ref);
        RequestBody expectedRequestBody = resolve(context,
            context.getExpectedOpenAPIJXPathContext(),
            expectedOperation.getRequestBody(),
            RequestBody::get$ref);
        if (expectedRequestBody != null && expectedRequestBody.getContent() != null) {
            for (Map.Entry<String, MediaType> entry : expectedRequestBody.getContent().entrySet()) {
                MediaType expectedMediaType = entry.getValue();
                MediaType actualMediaType = actualRequestBody != null && actualRequestBody
                    .getContent() != null ? actualRequestBody.getContent().get(entry.getKey()) : null;
                if (expectedMediaType != null || actualMediaType != null) {
                    if (actualMediaType == null) {
                        addMethodError(context,
                            String.format(
                                "The operation '%s' and media type '%s' related to the path item '%s' is found, but it is not specified in the OpenAPI file.",
                                context.getOperationType(),
                                entry.getKey(),
                                context.getPath()));
                    } else if (expectedMediaType == null) {
                        addMethodError(context,
                            String.format(
                                "The operation '%s' and media type '%s' related to the path item '%s' is not found, but it is specified in the OpenAPI file.",
                                context.getOperationType(),
                                entry.getKey(),
                                context.getPath()));
                    } else {
                        try {
                            context.setActualMediaType(actualMediaType);
                            context.setExpectedMediaType(expectedMediaType);
                            context.setMediaType(entry.getKey());
                            validateMediaType(context);
                        } finally {
                            context.setActualMediaType(null);
                            context.setExpectedMediaType(null);
                            context.setMediaType(null);
                        }
                    }
                }
            }
        }
        if (actualRequestBody != null && actualRequestBody.getContent() != null) {
            for (Map.Entry<String, MediaType> entry : actualRequestBody.getContent().entrySet()) {
                MediaType expectedMediaType = expectedRequestBody != null && expectedRequestBody
                    .getContent() != null ? expectedRequestBody.getContent().get(entry.getKey()) : null;
                if (expectedMediaType == null) {
                    addMethodError(context,
                        String.format(
                            "The operation '%s' and media type '%s' related to the path item '%s' is not specified in the OpenAPI file.",
                            context.getOperationType(),
                            entry.getKey(),
                            context.getPath()));
                }
            }
        }
        if (expectedOperation.getCallbacks() != null && !expectedOperation.getCallbacks().isEmpty()) {
            addMethodWarning(context,
                String.format(
                    "Out-of band callback related to the operation '%s' and the path item '%s' is ignored, because callbacks are not supported.",
                    context.getOperationType(),
                    context.getPath()));
        }
    }

    @SuppressWarnings("rawtypes")
    private void validateMediaType(Context context) {
        MediaType expectedMediaType = context.getExpectedMediaType();
        MediaType actualMediaType = context.getActualMediaType();
        IOpenMethod openMethod = context.getOpenMethod();
        Schema<?> expectedSchema = resolve(context,
            context.getExpectedOpenAPIJXPathContext(),
            expectedMediaType.getSchema(),
            Schema::get$ref);
        Schema<?> actualSchema = resolve(context,
            context.getActualOpenAPIJXPathContext(),
            actualMediaType.getSchema(),
            Schema::get$ref);
        if (expectedSchema == null) {
            addMethodError(context,
                String.format(
                    "Missed mandatory schema definition for the operation '%s' and media type '%s' related to the path item '%s' in the OpenAPI file.",
                    context.getOperationType(),
                    context.getMediaType(),
                    context.getPath()));
            return;
        }
        Method method = context.getMethodMap().get(context.getMethod());
        String[] parameterNames = MethodUtils.getParameterNames(
            isResolveMethodParameterNames() ? context.getOpenClass() : null,
            method,
            context.isProvideRuntimeContext(),
            context.isProvideRuntimeContext());
        if (method.getParameterCount() > 1 && context.getMethod().getParameterCount() == 1) {
            Map<String, Schema> allPropertiesOfActualSchema = extractAllProperties(context,
                context.getActualOpenAPIJXPathContext(),
                actualSchema);
            Map<String, Schema> allPropertiesOfExpectedSchema = extractAllProperties(context,
                context.getExpectedOpenAPIJXPathContext(),
                expectedSchema);
            int i = 0;
            for (String parameterName : parameterNames) {
                Schema<?> parameterSchema = allPropertiesOfActualSchema.get(parameterName);
                Class<?> parameterType = method.getParameterTypes()[i];
                IOpenClass parameterOpenClass = extractParameterOpenClass(context,
                    openMethod,
                    method,
                    i,
                    parameterType);
                Schema<?> expectedParameterSchema = allPropertiesOfExpectedSchema.get(parameterNames[i]);
                if (expectedParameterSchema == null) {
                    addMethodError(context,
                        String.format("The parameter '%s' of the method '%s' is not specified in the OpenAPI file.",
                            parameterNames[i],
                            method.getName()));
                    i++;
                    continue;
                }

                if (isIncompatibleTypes(parameterSchema, expectedParameterSchema, parameterOpenClass)) {
                    addMethodError(context,
                        String.format(
                            "The schema type of the parameter '%s' of the method '%s' is declared as '%s' that mismatches to the schema type '%s' specified in the OpenAPI file.",
                            parameterNames[i],
                            method.getName(),
                            resolveType(parameterSchema),
                            resolveType(expectedParameterSchema)));
                    i++;
                    continue;
                }
                try {
                    context.setTypeValidationInProgress(true);
                    validateType(context,
                        parameterSchema,
                        expectedParameterSchema,
                        parameterOpenClass,
                        new HashSet<>());
                } finally {
                    context.setTypeValidationInProgress(false);
                }
                i++;
            }
            for (Map.Entry<String, Schema> entry : allPropertiesOfExpectedSchema.entrySet()) {
                if (allPropertiesOfActualSchema.get(entry.getKey()) == null) {
                    addMethodError(context,
                        String.format(
                            "The parameter '%s' of the method '%s' is not found, but it is specified in the OpenAPI file.",
                            entry.getKey(),
                            method.getName()));
                }
            }
        } else {
            if (method.getParameterCount() > 0) {
                int i = 0;
                for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
                    Class<?> parameterType = method.getParameterTypes()[i];
                    IOpenClass parameterOpenClass = extractParameterOpenClass(context,
                        openMethod,
                        method,
                        i,
                        parameterType);
                    if (!isNonRequestBodyParameter(parameterAnnotations)) {
                        if (isIncompatibleTypes(actualSchema, expectedSchema, parameterOpenClass)) {
                            addMethodError(context,
                                String.format(
                                    "The schema type of the parameter '%s' of the method '%s' is declared as '%s' that mismatches to the schema type '%s' specified in the OpenAPI file.",
                                    parameterNames[i],
                                    method.getName(),
                                    resolveType(actualSchema),
                                    resolveType(expectedSchema)));
                        } else {
                            try {
                                context.setTypeValidationInProgress(true);
                                validateType(context,
                                    actualSchema,
                                    expectedSchema,
                                    parameterOpenClass,
                                    new HashSet<>());
                            } finally {
                                context.setTypeValidationInProgress(false);
                            }
                        }
                        return;
                    }
                    i++;
                }

            }
        }
    }

    private IOpenClass extractParameterOpenClass(Context context,
            IOpenMethod openMethod,
            Method method,
            int index,
            Class<?> parameterType) {
        IOpenClass parameterOpenClass = null;
        if (!(context.isProvideVariations() && VariationsPack.class == parameterType && index == method
            .getParameterCount() - 1)) {
            if (context.isProvideRuntimeContext()) {
                if (index > 0) {
                    parameterOpenClass = openMethod.getSignature().getParameterType(index - 1);
                }
            } else {
                parameterOpenClass = openMethod.getSignature().getParameterType(index);
            }
        }
        return parameterOpenClass == null ? JavaOpenClass.getOpenClass(parameterType) : parameterOpenClass;
    }

    private boolean isNonRequestBodyParameter(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof PathParam || annotation instanceof QueryParam || annotation instanceof CookieParam || annotation instanceof FormParam || annotation instanceof BeanParam || annotation instanceof HeaderParam || annotation instanceof MatrixParam) {
                return true;
            }
        }
        return false;
    }

    private boolean isFormParameter(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof FormParam) {
                return true;
            }
        }
        return false;
    }

    private boolean isIncompatibleTypes(Schema<?> parameterSchema,
            Schema<?> expectedParameterSchema,
            IOpenClass parameterOpenClass) {
        String expectedParameterSchemaType = resolveType(expectedParameterSchema);
        String actualParameterSchemaType = resolveType(parameterSchema);
        return !Objects.equals(expectedParameterSchemaType,
            actualParameterSchemaType) && expectedParameterSchemaType != null && actualParameterSchemaType != null && (isSimpleType(
                expectedParameterSchemaType) || isSimpleType(
                    actualParameterSchemaType) || parameterOpenClass instanceof DatatypeOpenClass && Objects
                        .equals(expectedParameterSchema.getName(), parameterSchema.getName()));
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Schema> extractAllProperties(Context context, JXPathContext jxPathContext, Schema<?> schema) {
        Schema<?> resolvedSchema = resolve(context, jxPathContext, schema, Schema::get$ref);
        if (resolvedSchema != null) {
            Map<String, Schema> properties = new HashMap<>();
            if (resolvedSchema instanceof ComposedSchema) {
                ComposedSchema composedSchema = (ComposedSchema) resolvedSchema;
                if (composedSchema.getAllOf() != null && !composedSchema.getAllOf().isEmpty()) {
                    for (Schema<?> embeddedSchema : composedSchema.getAllOf()) {
                        Map<String, Schema> embeddedSchemaProperties = extractAllProperties(context,
                            jxPathContext,
                            embeddedSchema);
                        if (embeddedSchemaProperties != null) {
                            properties.putAll(embeddedSchemaProperties);
                        }
                    }
                }
            } else {
                if (resolvedSchema.getProperties() != null) {
                    properties.putAll(resolvedSchema.getProperties());
                }
            }
            return properties;
        } else {
            return schema.getProperties() != null ? schema.getProperties() : Collections.emptyMap();
        }
    }

    private boolean isSimpleType(String type) {
        return "String".equals(type) || "Float".equals(type) || "Double".equals(type) || "Integer"
            .equals(type) || "Long".equals(type) || "Boolean".equals(type) || "Date".equals(type);
    }

    private String resolveType(Schema<?> schema) {
        if (schema.get$ref() != null) {
            return RefUtils.computeDefinitionName(schema.get$ref());
        }
        if ("string".equals(schema.getType())) {
            if ("date".equals(schema.getFormat())) {
                return "Date";
            } else if ("date-time".equals(schema.getFormat())) {
                return "Date";
            }
            return "String";
        } else if ("number".equals(schema.getType())) {
            if ("float".equals(schema.getFormat())) {
                return "Float";
            } else if ("double".equals(schema.getFormat())) {
                return "Double";
            } else {
                return "Double";
            }
        } else if ("integer".equals(schema.getType())) {
            if ("int32".equals(schema.getFormat())) {
                return "Integer";
            } else if ("int64".equals(schema.getFormat())) {
                return "Long";
            } else {
                return "Long";
            }
        } else if ("boolean".equals(schema.getType())) {
            return "Boolean";
        } else if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            String type = resolveType(arraySchema.getItems());
            return type != null ? type + "[]" : null;
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private void validateType(Context context,
            Schema<?> actualSchema,
            Schema<?> expectedSchema,
            IOpenClass openClass,
            Set<Schema<?>> validatedSchemas) {
        IOpenClass oldType = context.getType();
        try {
            Schema<?> resolvedActualSchema = resolve(context,
                context.getActualOpenAPIJXPathContext(),
                actualSchema,
                Schema::get$ref);
            if (resolvedActualSchema != null) {
                Schema<?> resolvedExpectedSchema = resolve(context,
                    context.getExpectedOpenAPIJXPathContext(),
                    expectedSchema,
                    Schema::get$ref);
                if (resolvedExpectedSchema != null) {
                    validatedSchemas.add(resolvedExpectedSchema);
                } else {
                    return;
                }
                context.setType(openClass);
                Map<String, Schema> allPropertiesOfExpectedSchema = extractAllProperties(context,
                    context.getExpectedOpenAPIJXPathContext(),
                    expectedSchema);
                Map<String, Schema> allPropertiesOfActualSchema = extractAllProperties(context,
                    context.getActualOpenAPIJXPathContext(),
                    resolvedActualSchema);

                for (Map.Entry<String, Schema> entry : allPropertiesOfExpectedSchema.entrySet()) {
                    IOpenField openField = openClass.getField(entry.getKey());
                    if (openField == null || openField.isTransient()) {
                        if (openField == null) {
                            addTypeError(context,
                                String.format(
                                    "The field '%s' is not found in the type '%s', but it is specified in the OpenAPI file.",
                                    entry.getKey(),
                                    openClass.getDisplayName(INamedThing.REGULAR)));
                        } else {
                            addTypeError(context,
                                String.format(
                                    "The field '%s' of the type '%s' is transient, but it is specified in the OpenAPI file.",
                                    entry.getKey(),
                                    openClass.getDisplayName(INamedThing.REGULAR)));
                        }
                    } else {
                        Schema<?> fieldActualSchema = allPropertiesOfActualSchema.get(entry.getKey());
                        if (fieldActualSchema == null) {
                            throw new IllegalStateException("Failed to resolve a reference for the generated schema");
                        } else {
                            if (isIncompatibleTypes(fieldActualSchema, entry.getValue(), openField.getType())) {
                                addTypeError(context,
                                    String.format(
                                        "The schema type of the property '%s' declared in the type '%s' is '%s' that mismatches to the type '%s' specified in the OpenAPI file.",
                                        entry.getKey(),
                                        openClass.getDisplayName(INamedThing.REGULAR),
                                        resolveType(fieldActualSchema),
                                        resolveType(entry.getValue())));
                            } else {
                                validateType(context,
                                    fieldActualSchema,
                                    entry.getValue(),
                                    openField.getType(),
                                    validatedSchemas);
                            }
                        }
                    }
                }

                for (Map.Entry<String, Schema> entry : allPropertiesOfActualSchema.entrySet()) {
                    Schema<?> fieldExpectedSchema = allPropertiesOfExpectedSchema.get(entry.getKey());
                    if (fieldExpectedSchema == null) {
                        addTypeWarning(context,
                            openClass,
                            String.format(
                                "The field '%s' is declared in the type '%s', but it is not specified in the OpenAPI file.",
                                entry.getKey(),
                                openClass.getDisplayName(INamedThing.REGULAR)));
                    }
                }
            }
        } finally {
            context.setType(oldType);
        }
    }

    private void validatePathItem(Context context) {
        getAndValidateOperation(context, PathItem::getGet, "GET");
        getAndValidateOperation(context, PathItem::getPost, "POST");
        getAndValidateOperation(context, PathItem::getDelete, "DELETE");
        getAndValidateOperation(context, PathItem::getPut, "PUT");
        getAndValidateOperation(context, PathItem::getTrace, "TRACE");
        getAndValidateOperation(context, PathItem::getHead, "HEAD");
    }

    public boolean isResolveMethodParameterNames() {
        return resolveMethodParameterNames;
    }

    public void setResolveMethodParameterNames(boolean resolveMethodParameterNames) {
        this.resolveMethodParameterNames = resolveMethodParameterNames;
    }
}
