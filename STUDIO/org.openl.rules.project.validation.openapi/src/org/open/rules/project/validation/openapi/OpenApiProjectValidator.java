package org.open.rules.project.validation.openapi;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.openl.CompiledOpenClass;
import org.openl.base.INamedThing;
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
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
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

    private OpenAPI loadOpenAPI(ProjectDescriptor projectDescriptor,
            ValidatedCompiledOpenClass validatedCompiledOpenClass) {
        ProjectResourceLoader projectResourceLoader = new ProjectResourceLoader(validatedCompiledOpenClass);
        String openApiFile = OPENAPI_JSON;
        ProjectResource projectResource = loadProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_JSON);
        if (projectResource == null) {
            openApiFile = OPENAPI_YAML;
            projectResource = loadProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_YAML);
        }
        if (projectResource == null) {
            openApiFile = OPENAPI_YML;
            projectResource = loadProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_YML);
        }
        if (projectResource != null) {
            OpenAPIParser openApiParser = new OpenAPIParser();
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            options.setFlatten(true);
            try {
                String content = new String(Files.readAllBytes(Paths.get(projectResource.getFile())));
                OpenAPI openAPI = openApiParser.readContents(content, null, options).getOpenAPI();
                if (openAPI == null) {
                    validatedCompiledOpenClass.addValidationMessage(
                        OpenLMessagesUtils.newErrorMessage(String.format("Invalid file '%s' format.", openApiFile)));
                }
                return openAPI;
            } catch (IOException e) {
                validatedCompiledOpenClass.addValidationMessage(
                    OpenLMessagesUtils.newErrorMessage(String.format("Failed to read '%s' file.", openApiFile)));
            }
        }
        return null;
    }

    @Override
    public CompiledOpenClass validate(ProjectDescriptor projectDescriptor,
            RulesInstantiationStrategy rulesInstantiationStrategy) throws RulesInstantiationException {
        final CompiledOpenClass compiledOpenClass = rulesInstantiationStrategy.compile();
        final ValidatedCompiledOpenClass validatedCompiledOpenClass = new OpenApiValidatedCompiledOpenClass(
            compiledOpenClass);
        OpenAPI expectedOpenAPI = loadOpenAPI(projectDescriptor, validatedCompiledOpenClass);
        if (expectedOpenAPI == null) {
            return validatedCompiledOpenClass;
        }
        final Context context = new Context();
        context.setExpectedOpenAPI(expectedOpenAPI);
        context.setExpectedOpenAPIResolver(new OpenAPIResolver(context, expectedOpenAPI));
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
        context.setObjectMapper(objectMapper);
        synchronized (OpenApiRulesCacheWorkaround.class) {
            OpenApiRulesCacheWorkaround.reset();
            OpenApiObjectMapperHack openApiObjectMapperHack = new OpenApiObjectMapperHack();
            openApiObjectMapperHack.apply(objectMapper);
            actualOpenAPI = reader.read(enhancedServiceClass);
            openApiObjectMapperHack.revert();
        }
        context.setActualOpenAPI(actualOpenAPI);
        context.setActualOpenAPIResolver(new OpenAPIResolver(context, actualOpenAPI));
        validateOpenAPI(context);
        return validatedCompiledOpenClass;
    }

    public interface Service {
        @POST
        @Path("/method")
        void method(String s);
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

    @SuppressWarnings("rawtypes")
    private void validateOpenAPI(Context context) {
        if (context.getExpectedOpenAPI().getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : context.getExpectedOpenAPI().getPaths().entrySet()) {
                PathItem expectedPathItem = entry.getValue();
                try {
                    context.setPath(entry.getKey());
                    context.setExpectedPathItem(expectedPathItem);
                    if (context.getActualOpenAPI().getPaths() != null) {
                        PathItem actualPathItem = context.getActualOpenAPI().getPaths().get(entry.getKey());
                        if (actualPathItem != null) {
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
                            OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                                String.format(
                                    "There is no method found for the path '%s', but the path is specified in the OpenAPI file.",
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
                try {
                    context.setActualPathItem(entry.getValue());
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
                    PathItem expectedPathItem = context.getExpectedOpenAPI().getPaths().get(entry.getKey());
                    context.setExpectedPathItem(expectedPathItem);
                    if (expectedPathItem == null) {
                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                            String.format(
                                "The method '%s' related to the path '%s' is found, but it is not specified in the OpenAPI file.",
                                openMethod.getName(),
                                entry.getKey()));
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

        if (context.getExpectedOpenAPI().getComponents() != null && context.getActualOpenAPI()
            .getComponents() != null) {
            Components expectedComponents = context.getExpectedOpenAPI().getComponents();
            Components actualComponents = context.getActualOpenAPI().getComponents();
            if (expectedComponents.getSchemas() != null && actualComponents.getSchemas() != null) {
                for (Map.Entry<String, Schema> entry : expectedComponents.getSchemas().entrySet()) {
                    Schema actualSchema = actualComponents.getSchemas().get(entry.getKey());
                    Schema expectedSchema = entry.getValue();
                    IOpenClass type = context.getOpenClass().findType(entry.getKey());
                    if (actualSchema != null && expectedSchema != null && type != null) {
                        try {
                            context.setTypeValidationInProgress(true);
                            // validateType(context, actualSchema, expectedSchema, type, new HashSet<>());
                        } finally {
                            context.setTypeValidationInProgress(false);
                        }
                    }
                }
            }
        }
    }

    private void getAndValidateOperation(Context context,
            java.util.function.Function<PathItem, Operation> func,
            String operationType) {
        PathItem actualPathItem = context.getActualPathItem();
        PathItem expectedPathItem = context.getExpectedPathItem();

        Operation expectedOperation = func.apply(expectedPathItem);
        Operation actualOperation = func.apply(actualPathItem);

        if (expectedOperation != null || actualOperation != null) {
            if (actualOperation == null) {
                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                    String.format(
                        "The operation '%s' related to the path '%s' is found, but it is not specified in the OpenAPI file.",
                        operationType,
                        context.getPath()));
            } else if (expectedOperation == null) {
                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                    String.format(
                        "The operation '%s' related to the path '%s' is not found, but it is specified in the OpenAPI file.",
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

    private void validateOperation(Context context) {
        Operation expectedOperation = context.getExpectedOperation();
        Operation actualOperation = context.getActualOperation();
        RequestBody actualRequestBody = context.getActualOpenAPIResolver()
            .resolve(actualOperation.getRequestBody(), RequestBody::get$ref);
        RequestBody expectedRequestBody = context.getExpectedOpenAPIResolver()
            .resolve(expectedOperation.getRequestBody(), RequestBody::get$ref);
        if (expectedRequestBody != null && expectedRequestBody.getContent() != null) {
            for (Map.Entry<String, MediaType> entry : expectedRequestBody.getContent().entrySet()) {
                MediaType expectedMediaType = entry.getValue();
                MediaType actualMediaType = actualRequestBody != null && actualRequestBody
                    .getContent() != null ? actualRequestBody.getContent().get(entry.getKey()) : null;
                if (expectedMediaType != null || actualMediaType != null) {
                    if (actualMediaType == null) {
                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                            String.format(
                                "The request body for the operation '%s' and media type '%s' related to the path '%s' is found, but it is not specified in the OpenAPI file.",
                                context.getOperationType(),
                                entry.getKey(),
                                context.getPath()));
                    } else if (expectedMediaType == null) {
                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                            String.format(
                                "The request body for the operation '%s' and media type '%s' related to the path '%s' is not found, but it is specified in the OpenAPI file.",
                                context.getOperationType(),
                                entry.getKey(),
                                context.getPath()));
                    } else {
                        try {
                            context.setActualMediaType(actualMediaType);
                            context.setExpectedMediaType(expectedMediaType);
                            context.setMediaType(entry.getKey());
                            validateInput(context);
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
                    OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                        String.format(
                            "The operation '%s' and media type '%s' related to the path '%s' is found, but it is not specified in the OpenAPI file.",
                            context.getOperationType(),
                            entry.getKey(),
                            context.getPath()));
                }
            }
        }

        if (expectedOperation.getResponses() != null || actualOperation.getResponses() != null) {
            if (expectedOperation.getResponses() == null) {
                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                    String.format(
                        "The response for the operation '%s' related to the path '%s' is found, but it is not specified in the OpenAPI file.",
                        context.getOperationType(),
                        context.getPath()));
            } else if (actualOperation.getResponses() == null) {
                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                    String.format(
                        "The response for the operation '%s' related to the path '%s' is not found, but it is specified in the OpenAPI file.",
                        context.getOperationType(),
                        context.getPath()));
            } else {
                ApiResponse expectedApiResponse = expectedOperation.getResponses().getDefault();
                if (expectedApiResponse == null) {
                    expectedApiResponse = expectedOperation.getResponses().get("200");
                }
                ApiResponse actualApiResponse = actualOperation.getResponses().getDefault();
                if (actualApiResponse == null) {
                    actualApiResponse = actualOperation.getResponses().get("200");
                }
                expectedApiResponse = context.getExpectedOpenAPIResolver()
                    .resolve(expectedApiResponse, ApiResponse::get$ref);
                actualApiResponse = context.getActualOpenAPIResolver().resolve(actualApiResponse, ApiResponse::get$ref);
                if (expectedApiResponse != null || actualApiResponse != null) {
                    if (expectedApiResponse == null) {
                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                            String.format(
                                "The response for the operation '%s' related to the path '%s' is found, but it is not specified in the OpenAPI file.",
                                context.getOperationType(),
                                context.getPath()));
                    } else if (actualApiResponse == null) {
                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                            String.format(
                                "The response for the operation '%s' related to the path '%s' is not found, but it is specified in the OpenAPI file.",
                                context.getOperationType(),
                                context.getPath()));

                    } else {
                        if (expectedApiResponse.getContent() != null) {
                            for (Map.Entry<String, MediaType> entry : expectedApiResponse.getContent().entrySet()) {
                                MediaType expectedMediaType = entry.getValue();
                                MediaType actualMediaType = actualApiResponse.getContent().get(entry.getKey());
                                if (expectedMediaType != null || actualMediaType != null) {
                                    if (actualMediaType == null) {
                                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                                            String.format(
                                                "The response for the operation '%s' and media type '%s' related to the path '%s' is found, but it is not specified in the OpenAPI file.",
                                                context.getOperationType(),
                                                entry.getKey(),
                                                context.getPath()));
                                    } else if (expectedMediaType == null) {
                                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                                            String.format(
                                                "The response for the operation '%s' and media type '%s' related to the path '%s' is not found, but it is specified in the OpenAPI file.",
                                                context.getOperationType(),
                                                entry.getKey(),
                                                context.getPath()));
                                    } else {
                                        try {
                                            context.setActualMediaType(actualMediaType);
                                            context.setExpectedMediaType(expectedMediaType);
                                            context.setMediaType(entry.getKey());
                                            validateResponse(context);
                                        } finally {
                                            context.setActualMediaType(null);
                                            context.setExpectedMediaType(null);
                                            context.setMediaType(null);
                                        }
                                    }
                                }
                            }
                        }
                        if (actualApiResponse.getContent() != null) {
                            for (Map.Entry<String, MediaType> entry : actualApiResponse.getContent().entrySet()) {
                                MediaType expectedMediaType = expectedApiResponse
                                    .getContent() != null ? expectedApiResponse.getContent().get(entry.getKey()) : null;
                                if (expectedMediaType == null) {
                                    OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                                        String.format(
                                            "The response for the operation '%s' and media type '%s' related to the path '%s' is found, but it is not specified in the OpenAPI file.",
                                            context.getOperationType(),
                                            entry.getKey(),
                                            context.getPath()));
                                }
                            }
                        }
                    }
                }
            }
        }
        if (expectedOperation.getCallbacks() != null && !expectedOperation.getCallbacks().isEmpty()) {
            OpenApiProjectValidatorMessagesUtils.addMethodWarning(context,
                String.format(
                    "Out-of band callback related to the operation '%s' and the path '%s' in the OpenAPI file is ignored. Callbacks are not supported.",
                    context.getOperationType(),
                    context.getPath()));
        }
    }

    private void validateResponse(Context context) {
        MediaType expectedMediaType = context.getExpectedMediaType();
        MediaType actualMediaType = context.getActualMediaType();
        IOpenMethod openMethod = context.getOpenMethod();
        Schema<?> expectedSchema = context.getExpectedOpenAPIResolver()
            .resolve(expectedMediaType.getSchema(), Schema::get$ref);
        Schema<?> actualSchema = context.getActualOpenAPIResolver()
            .resolve(actualMediaType.getSchema(), Schema::get$ref);
        if (expectedSchema == null) {
            OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                String.format(
                    "Missed a schema definition for the response in the operation '%s' and media type '%s' related to the path '%s' in the OpenAPI file.",
                    context.getOperationType(),
                    context.getMediaType(),
                    context.getPath()));
            return;
        }
        Method method = context.getMethodMap().get(context.getMethod());
        IOpenClass returnType = method.getReturnType() == openMethod.getType().getInstanceClass() ? openMethod
            .getType() : JavaOpenClass.getOpenClass(method.getReturnType());
        if (isIncompatibleTypes(actualSchema, expectedSchema, returnType)) {
            OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                String.format(
                    "The method '%s' related to the path '%s' has response schema type '%s' that mismatches to the schema type '%s' specified in the OpenAPI file.",
                    method.getName(),
                    context.getPath(),
                    resolveType(actualMediaType.getSchema()),
                    resolveType(expectedMediaType.getSchema())));
        } else {
            try {
                context.setTypeValidationInProgress(true);
                try {
                    validateType(context, actualSchema, expectedSchema, returnType, new HashSet<>());
                } catch (DifferentTypesException e) {
                    String resolvedActualSchemaType = resolveType(actualMediaType.getSchema());
                    String resolvedExpectedSchemaType = resolveType(expectedMediaType.getSchema());
                    OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                        String.format(
                            "The response schema type%s of the method '%s' related to the path '%s' mismatches to the schema type%s specified in the OpenAPI file.",
                            resolvedActualSchemaType != null ? " '" + resolvedActualSchemaType + "'" : "",
                            method.getName(),
                            context.getPath(),
                            resolvedExpectedSchemaType != null ? " '" + resolvedExpectedSchemaType + "'" : ""));
                }
            } finally {
                context.setTypeValidationInProgress(false);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void validateInput(Context context) {
        MediaType expectedMediaType = context.getExpectedMediaType();
        MediaType actualMediaType = context.getActualMediaType();
        IOpenMethod openMethod = context.getOpenMethod();
        Schema<?> expectedSchema = context.getExpectedOpenAPIResolver()
            .resolve(expectedMediaType.getSchema(), Schema::get$ref);
        Schema<?> actualSchema = context.getActualOpenAPIResolver()
            .resolve(actualMediaType.getSchema(), Schema::get$ref);
        if (expectedSchema == null) {
            OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                String.format(
                    "Schema definition for the operation '%s' and media type '%s' related to the path '%s' is found, but it is not specified in the OpenAPI file.",
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

        Map<String, Schema> allPropertiesOfActualSchema = context.getActualOpenAPIResolver()
            .resolveAllProperties(actualSchema);

        Map<String, Schema> allPropertiesOfExpectedSchema = context.getExpectedOpenAPIResolver()
            .resolveAllProperties(expectedSchema);

        if (method.getParameterCount() > 1 && context.getMethod().getParameterCount() == 1) {
            int i = 0;
            for (String parameterName : parameterNames) {
                Schema<?> parameterSchema = allPropertiesOfActualSchema.get(parameterName);
                Class<?> parameterType = method.getParameterTypes()[i];
                IOpenClass parameterOpenClass = extractParameterOpenClass(context,
                    openMethod,
                    method,
                    i,
                    parameterType);
                Schema<?> expectedParameterSchema = allPropertiesOfExpectedSchema.get(parameterName);
                validateMethodParameter(context,
                    method,
                    parameterName,
                    parameterSchema,
                    parameterOpenClass,
                    expectedParameterSchema);
                i++;
            }
            for (Map.Entry<String, Schema> entry : allPropertiesOfExpectedSchema.entrySet()) {
                if (allPropertiesOfActualSchema.get(entry.getKey()) == null) {
                    OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                        String.format(
                            "The method '%s' related to the path '%s' does not have the parameter '%s', but the parameter is specified with type '%s' in the OpenAPI file.",
                            method.getName(),
                            context.getPath(),
                            entry.getKey(),
                            resolveType(entry.getValue())));
                }
            }
        } else {
            if (method.getParameterCount() == 1 && isJAXRSBeanParamAnnotationPresented(
                method.getParameterAnnotations()[0])) {
                IOpenClass parameterOpenClass = extractParameterOpenClass(context,
                    openMethod,
                    method,
                    0,
                    method.getParameterTypes()[0]);
                validateMethodParameter(context,
                    method,
                    parameterNames[0],
                    actualSchema,
                    parameterOpenClass,
                    expectedSchema);
            } else if (method.getParameterCount() > 0) {
                int i = 0;
                for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
                    Class<?> parameterType = method.getParameterTypes()[i];
                    IOpenClass parameterOpenClass = extractParameterOpenClass(context,
                        openMethod,
                        method,
                        i,
                        parameterType);
                    String parameterName = parameterNames[i];
                    if (!isJAXRSParameterAnnotationPresented(parameterAnnotations)) {
                        validateMethodParameter(context,
                            method,
                            parameterName,
                            actualSchema,
                            parameterOpenClass,
                            expectedSchema);
                        return;
                    }
                    i++;
                }
                i = 0;
                for (String parameterName : parameterNames) {
                    Schema<?> parameterSchema = allPropertiesOfActualSchema.get(parameterName);
                    Class<?> parameterType = method.getParameterTypes()[i];
                    boolean isJAXRSFormParamAnnotationPresented = isJAXRSFormParamAnnotationPresented(
                        method.getParameterAnnotations()[i]);
                    if (isJAXRSFormParamAnnotationPresented) {
                        IOpenClass parameterOpenClass = extractParameterOpenClass(context,
                            openMethod,
                            method,
                            i,
                            parameterType);
                        Schema<?> expectedParameterSchema = allPropertiesOfExpectedSchema.get(parameterName);
                        validateMethodParameter(context,
                            method,
                            parameterName,
                            parameterSchema,
                            parameterOpenClass,
                            expectedParameterSchema);
                    }
                    i++;
                }
            }
        }
    }

    private void validateMethodParameter(Context context,
            Method method,
            String parameterName,
            Schema<?> parameterSchema,
            IOpenClass parameterOpenClass,
            Schema<?> expectedParameterSchema) {
        if (expectedParameterSchema == null) {
            OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                String.format(
                    "The method '%s' related to the path '%s' is declared with the parameter '%s', but the parameter for this method is not specified in the OpenAPI file.",
                    method.getName(),
                    context.getPath(),
                    parameterName));
        } else {
            if (isIncompatibleTypes(parameterSchema, expectedParameterSchema, parameterOpenClass)) {
                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                    String.format(
                        "The method '%s' related to the path '%s' has the parameter '%s' with schema type '%s' that mismatches to the schema type '%s' specified in the OpenAPI file.",
                        method.getName(),
                        context.getPath(),
                        parameterName,
                        resolveType(parameterSchema),
                        resolveType(expectedParameterSchema)));
            } else {
                try {
                    context.setTypeValidationInProgress(true);
                    try {
                        validateType(context,
                            parameterSchema,
                            expectedParameterSchema,
                            parameterOpenClass,
                            new HashSet<>());
                    } catch (DifferentTypesException e) {
                        String resolvedParameterSchemaType = resolveType(parameterSchema);
                        String resolvedExpectedParameterSchemaType = resolveType(expectedParameterSchema);
                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                            String.format(
                                "The method '%s' related to the path '%s' has the parameter '%s' with schema type%s that mismatches to the schema type%s specified in the OpenAPI file.",
                                method.getName(),
                                context.getPath(),
                                parameterName,
                                resolvedParameterSchemaType != null ? " '" + resolvedParameterSchemaType + "'" : "",
                                resolvedExpectedParameterSchemaType != null ? " '" + resolvedExpectedParameterSchemaType + "'"
                                                                            : ""));
                    }
                } finally {
                    context.setTypeValidationInProgress(false);
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

    private boolean isJAXRSParameterAnnotationPresented(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof PathParam || annotation instanceof QueryParam || annotation instanceof CookieParam || annotation instanceof FormParam || annotation instanceof BeanParam || annotation instanceof HeaderParam || annotation instanceof MatrixParam) {
                return true;
            }
        }
        return false;
    }

    private boolean isJAXRSFormParamAnnotationPresented(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof FormParam) {
                return true;
            }
        }
        return false;
    }

    private boolean isJAXRSBeanParamAnnotationPresented(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof BeanParam) {
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

    interface Function {
        void run();
    }

    @SuppressWarnings("rawtypes")
    private void validateType(Context context,
            Schema<?> actualSchema,
            Schema<?> expectedSchema,
            IOpenClass openClass,
            Set<Schema<?>> validatedSchemas) throws DifferentTypesException {
        IOpenClass oldType = context.getType();
        try {
            Schema<?> resolvedActualSchema = context.getActualOpenAPIResolver().resolve(actualSchema, Schema::get$ref);
            if (resolvedActualSchema != null) {
                Schema<?> resolvedExpectedSchema = context.getExpectedOpenAPIResolver()
                    .resolve(expectedSchema, Schema::get$ref);
                if (resolvedExpectedSchema != null) {
                    if (validatedSchemas.contains(resolvedActualSchema)) {
                        return;
                    } else {
                        validatedSchemas.add(resolvedExpectedSchema);
                    }
                } else {
                    return;
                }
                context.setType(openClass);
                Map<String, Schema> allPropertiesOfExpectedSchema = context.getExpectedOpenAPIResolver()
                    .resolveAllProperties(expectedSchema);
                Map<String, Schema> allPropertiesOfActualSchema = context.getActualOpenAPIResolver()
                    .resolveAllProperties(resolvedActualSchema);
                List<Function> wrongFields = new ArrayList<>();
                int fieldsToValidate = 0;
                for (Map.Entry<String, Schema> entry : allPropertiesOfExpectedSchema.entrySet()) {
                    Schema<?> fieldActualSchema = allPropertiesOfActualSchema.get(entry.getKey());
                    if (fieldActualSchema == null) {
                        wrongFields.add(() -> OpenApiProjectValidatorMessagesUtils.addTypeError(context,
                            String.format(
                                "The field '%s' is not found in the type '%s' or the field is a transient, but it is specified in the OpenAPI file.",
                                entry.getKey(),
                                openClass.getDisplayName(INamedThing.REGULAR))));
                    } else {
                        IOpenField openField = context.getOpenClassPropertiesResolver()
                            .getField(openClass, entry.getKey());
                        if (isIncompatibleTypes(fieldActualSchema, entry.getValue(), openField.getType())) {
                            wrongFields.add(() -> OpenApiProjectValidatorMessagesUtils.addTypeError(context,
                                String.format(
                                    "The schema type of the property '%s' declared in the type '%s' is '%s' that mismatches to the type '%s' specified in the OpenAPI file.",
                                    entry.getKey(),
                                    openClass.getDisplayName(INamedThing.REGULAR),
                                    resolveType(fieldActualSchema),
                                    resolveType(entry.getValue()))));
                        } else {
                            fieldsToValidate++;
                        }
                    }
                }
                if (fieldsToValidate == 0 && !wrongFields.isEmpty()) {
                    throw new DifferentTypesException();
                } else {
                    wrongFields.forEach(Function::run);
                }
                for (Map.Entry<String, Schema> entry : allPropertiesOfActualSchema.entrySet()) {
                    Schema<?> fieldExpectedSchema = allPropertiesOfExpectedSchema.get(entry.getKey());
                    if (fieldExpectedSchema == null) {
                        OpenApiProjectValidatorMessagesUtils.addTypeError(context,
                            String.format(
                                "The field '%s' is declared in the type '%s', but it is not specified in the OpenAPI file.",
                                entry.getKey(),
                                openClass.getDisplayName(INamedThing.REGULAR)));
                    }
                }
                for (Map.Entry<String, Schema> entry : allPropertiesOfExpectedSchema.entrySet()) {
                    Schema<?> fieldActualSchema = allPropertiesOfActualSchema.get(entry.getKey());
                    if (fieldActualSchema != null) {
                        IOpenField openField = context.getOpenClassPropertiesResolver()
                            .getField(openClass, entry.getKey());
                        if (!isIncompatibleTypes(fieldActualSchema, entry.getValue(), openField.getType())) {
                            try {
                                validateType(context,
                                    fieldActualSchema,
                                    entry.getValue(),
                                    openField.getType(),
                                    validatedSchemas);
                            } catch (DifferentTypesException e) {
                                String resolvedFieldActualSchemaType = resolveType(fieldActualSchema);
                                String resolvedExpectedFieldSchemaType = resolveType(entry.getValue());
                                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                                    String.format(
                                        "The schema type of the property '%s' declared in the type%s is '%s' that mismatches to the type%s specified in the OpenAPI file.",
                                        openField.getName(),
                                        openClass.getName(),
                                        resolvedFieldActualSchemaType != null ? " '" + resolvedFieldActualSchemaType + "'"
                                                                              : "",
                                        resolvedExpectedFieldSchemaType != null ? " '" + resolvedExpectedFieldSchemaType + "'"
                                                                                : ""));
                            }
                        }
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
