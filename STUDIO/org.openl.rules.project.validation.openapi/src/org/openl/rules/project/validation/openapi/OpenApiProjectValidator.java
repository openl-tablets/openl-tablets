package org.openl.rules.project.validation.openapi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.tuple.Pair;
import org.openl.CompiledOpenClass;
import org.openl.base.INamedThing;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
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
import org.openl.rules.ruleservice.publish.jaxrs.ParameterIndex;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.OpenApiObjectMapperHack;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.OpenApiRulesCacheWorkaround;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.SchemaJacksonObjectMapperFactoryBean;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson.OpenApiObjectMapperConfigurationHelper;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson.OpenApiObjectMapperFactory;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenField;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.util.RefUtils;

public class OpenApiProjectValidator extends AbstractServiceInterfaceProjectValidator {

    private static final String OPENAPI_JSON = "openapi.json";
    private static final String OPENAPI_YAML = "openapi.yaml";
    private static final String OPENAPI_YML = "openapi.yml";
    public static final String OPEN_API_VALIDATION_MSG_PREFIX = "OpenAPI Reconciliation: ";

    private boolean resolveMethodParameterNames = true;

    private final Reader reader = new Reader();

    private OpenAPI loadOpenAPI(Context context,
            ProjectDescriptor projectDescriptor,
            ValidatedCompiledOpenClass validatedCompiledOpenClass) {
        ProjectResourceLoader projectResourceLoader = new ProjectResourceLoader(validatedCompiledOpenClass);
        String openApiFile;
        ProjectResource projectResource;
        if (projectDescriptor.getOpenapi() != null && StringUtils
            .isNotBlank(projectDescriptor.getOpenapi().getPath())) {
            openApiFile = projectDescriptor.getOpenapi().getPath();
            projectResource = loadProjectResource(projectResourceLoader,
                projectDescriptor,
                projectDescriptor.getOpenapi().getPath());
            if (projectResource == null) {
                validatedCompiledOpenClass.addValidationMessage(OpenLMessagesUtils
                    .newErrorMessage(String.format(OPEN_API_VALIDATION_MSG_PREFIX + "File '%s' is not found.",
                        projectDescriptor.getOpenapi().getPath())));
            }
        } else {
            openApiFile = OPENAPI_JSON;
            projectResource = loadProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_JSON);
            if (projectResource == null) {
                openApiFile = OPENAPI_YAML;
                projectResource = loadProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_YAML);
            }
            if (projectResource == null) {
                openApiFile = OPENAPI_YML;
                projectResource = loadProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_YML);
            }
        }
        if (projectResource != null) {
            if (projectResource.getFile().endsWith(".yaml") || projectResource.getFile().endsWith(".yml")) {
                context.setYaml(true);
            }
            OpenAPIParser openApiParser = new OpenAPIParser();
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            OpenAPI openAPI = openApiParser.readLocation(projectResource.getUrl().toString(), null, options).getOpenAPI();
            if (openAPI == null) {
                validatedCompiledOpenClass.addValidationMessage(OpenLMessagesUtils.newErrorMessage(
                    String.format(OPEN_API_VALIDATION_MSG_PREFIX + "Failed to read file '%s'.", openApiFile)));
            }
            return openAPI;
        }
        return null;
    }

    @Override
    public CompiledOpenClass validate(ProjectDescriptor projectDescriptor,
            RulesInstantiationStrategy rulesInstantiationStrategy) throws RulesInstantiationException {
        final CompiledOpenClass compiledOpenClass = rulesInstantiationStrategy.compile();
        final ValidatedCompiledOpenClass validatedCompiledOpenClass = new OpenApiValidatedCompiledOpenClass(
            compiledOpenClass);
        final Context context = new Context();
        OpenAPI expectedOpenAPI = loadOpenAPI(context, projectDescriptor, validatedCompiledOpenClass);
        if (expectedOpenAPI == null) {
            return validatedCompiledOpenClass;
        }
        context.setExpectedOpenAPI(expectedOpenAPI);
        context.setExpectedOpenAPIResolver(new OpenAPIResolver(context, expectedOpenAPI));
        context.setValidatedCompiledOpenClass(validatedCompiledOpenClass);
        context.setOpenClass(validatedCompiledOpenClass.getOpenClassWithErrors());
        ClassLoader serviceClassLoader = resolveServiceClassLoader(rulesInstantiationStrategy);
        context.setServiceClassLoader(serviceClassLoader);
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(serviceClassLoader);
            Class<?> serviceClass = resolveInterface(projectDescriptor,
                rulesInstantiationStrategy,
                validatedCompiledOpenClass);
            Class<?> enhancedServiceClass;
            try {
                enhancedServiceClass = enhanceWithJAXRS(context,
                    projectDescriptor,
                    validatedCompiledOpenClass,
                    serviceClass,
                    serviceClassLoader);
            } catch (Exception e) {
                validatedCompiledOpenClass
                    .addValidationMessage(
                        OpenLMessagesUtils.newErrorMessage(OPEN_API_VALIDATION_MSG_PREFIX + String
                            .format("Failed to build an interface for the project.%s",
                                org.apache.commons.lang3.StringUtils
                                    .isNotBlank(e.getMessage()) ? " " + e.getMessage()
                                                                : org.apache.commons.lang3.StringUtils.EMPTY)));
                return validatedCompiledOpenClass;
            }
            context.setServiceClass(enhancedServiceClass);
            try {
                context
                    .setMethodMap(JAXRSOpenLServiceEnhancerHelper.buildMethodMap(serviceClass, enhancedServiceClass));
            } catch (Exception e) {
                validatedCompiledOpenClass.addValidationMessage(OpenLMessagesUtils
                    .newErrorMessage(OPEN_API_VALIDATION_MSG_PREFIX + "Failed to build an interface for the project."));
                return validatedCompiledOpenClass;
            }
            OpenAPI actualOpenAPI;
            RulesDeploy rulesDeploy = getRulesDeploy(projectDescriptor, compiledOpenClass);
            context.setRulesDeploy(rulesDeploy);
            ObjectMapper objectMapper = createObjectMapper(context);
            context.setObjectMapper(objectMapper);
            synchronized (OpenApiRulesCacheWorkaround.class) {
                OpenApiObjectMapperHack openApiObjectMapperHack = new OpenApiObjectMapperHack();
                try {
                    OpenApiRulesCacheWorkaround.reset();
                    openApiObjectMapperHack.apply(objectMapper);
                    actualOpenAPI = reader.read(enhancedServiceClass);
                } catch (Exception e) {
                    StringBuilder message = new StringBuilder(OPEN_API_VALIDATION_MSG_PREFIX);
                    message.append("Failed to build OpenAPI for the current project.");
                    if (StringUtils.isNotBlank(e.getMessage())) {
                        message.append(" ").append(e.getMessage());
                    }
                    validatedCompiledOpenClass
                        .addValidationMessage(OpenLMessagesUtils.newErrorMessage(message.toString()));
                    return validatedCompiledOpenClass;
                } finally {
                    OpenApiRulesCacheWorkaround.reset();
                    openApiObjectMapperHack.revert();
                }
            }
            context.setActualOpenAPI(actualOpenAPI);
            context.setActualOpenAPIResolver(new OpenAPIResolver(context, actualOpenAPI));
            validateOpenAPI(context);
            return validatedCompiledOpenClass;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private ObjectMapper createObjectMapper(Context context) {
        ClassLoader classLoader = context.getValidatedCompiledOpenClass().getClassLoader();

        SchemaJacksonObjectMapperFactoryBean objectMapperFactoryBean = new SchemaJacksonObjectMapperFactoryBean();
        objectMapperFactoryBean.setClassLoader(classLoader);
        objectMapperFactoryBean.setRulesDeploy(context.getRulesDeploy());
        objectMapperFactoryBean.setXlsModuleOpenClass((XlsModuleOpenClass) context.getOpenClass());
        objectMapperFactoryBean.setObjectMapperFactory(new OpenApiObjectMapperFactory());
        objectMapperFactoryBean.setClassLoader(context.getServiceClassLoader());
        try {
            ObjectMapper objectMapper = objectMapperFactoryBean.createJacksonObjectMapper();
            OpenApiObjectMapperConfigurationHelper.configure(objectMapper);
            return objectMapper;
        } catch (ClassNotFoundException e) { // Never happens
            throw new IllegalStateException("Failed to create an object mapper", e);
        }
    }

    private Class<?> enhanceWithJAXRS(Context context,
            ProjectDescriptor projectDescriptor,
            CompiledOpenClass compiledOpenClass,
            Class<?> originalClass,
            ClassLoader classLoader) throws Exception {
        RulesDeploy rulesDeploy = getRulesDeploy(projectDescriptor, compiledOpenClass);
        final boolean provideRuntimeContext = rulesDeploy == null && isProvideRuntimeContext() || rulesDeploy != null && Boolean.TRUE
            .equals(rulesDeploy.isProvideRuntimeContext());
        final boolean provideVariations = rulesDeploy == null && isProvideVariations() || rulesDeploy != null && Boolean.TRUE
            .equals(rulesDeploy.isProvideVariations());
        context.setProvideRuntimeContext(provideRuntimeContext);
        context.setProvideVariations(provideVariations);
        return JAXRSOpenLServiceEnhancerHelper.enhanceInterface(originalClass,
            compiledOpenClass.getOpenClassWithErrors(),
            classLoader,
            "unknown",
            "unknown",
            isResolveMethodParameterNames(),
            provideRuntimeContext,
            provideVariations);
    }

    private Pair<String, PathItem> findPathItem(io.swagger.v3.oas.models.Paths paths, String path) {
        if (paths != null) {
            path = path.replaceAll("\\{[^}]*}", "{}");
            for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
                String k = entry.getKey();
                k = k.replaceAll("\\{[^}]*}", "{}");
                if (Objects.equals(path, k)) {
                    return Pair.of(entry.getKey(), entry.getValue());
                }
            }
        }
        return null;
    }

    private void validateOpenAPI(Context context) {
        if (context.getActualOpenAPI().getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : context.getActualOpenAPI().getPaths().entrySet()) {
                try {
                    context.setActualPath(entry.getKey());
                    context.setActualPathItem(entry.getValue());
                    Pair<String, PathItem> expectedPath = findPathItem(context.getExpectedOpenAPI().getPaths(),
                        entry.getKey());
                    Method method = findMethodByPath(context.getServiceClass(), entry.getKey());
                    if (method == null) {
                        continue;
                    }
                    IOpenMethod openMethod = MethodUtils.findRulesMethod(context.getOpenClass(),
                        context.getMethodMap().get(method),
                        context.isProvideRuntimeContext(),
                        context.isProvideVariations());
                    if (expectedPath != null && expectedPath.getRight() != null) {
                        context.setExpectedPath(expectedPath.getKey());
                        context.setExpectedPathItem(expectedPath.getValue());
                        context.setMethod(method);
                        context.setOpenMethod(openMethod);
                        validatePathItem(context);
                    } else {
                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                            String.format(
                                OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected method '%s' is found for path '%s'.",
                                openMethod != null ? openMethod.getName() : method.getName(),
                                entry.getKey()));
                    }
                } finally {
                    context.setOpenMethod(null);
                    context.setMethod(null);
                    context.setActualPathItem(null);
                    context.setExpectedPathItem(null);
                    context.setExpectedPath(null);
                    context.setActualPath(null);
                }
            }
        }
        if (context.getExpectedOpenAPI().getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : context.getExpectedOpenAPI().getPaths().entrySet()) {
                PathItem expectedPathItem = entry.getValue();
                try {
                    context.setExpectedPath(entry.getKey());
                    context.setExpectedPathItem(expectedPathItem);
                    if (context.getActualOpenAPI().getPaths() != null) {
                        Pair<String, PathItem> actualPath = findPathItem(context.getActualOpenAPI().getPaths(),
                            entry.getKey());
                        if (actualPath == null) {
                            OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                                String.format(
                                    OPEN_API_VALIDATION_MSG_PREFIX + "Expected method is not found for path '%s'.",
                                    context.getExpectedPath()));
                        }
                    }
                } finally {
                    context.setExpectedPath(null);
                    context.setExpectedPathItem(null);
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
                        OPEN_API_VALIDATION_MSG_PREFIX + "Expected operation '%s' is not found for path '%s'.",
                        operationType,
                        context.getActualPath()));
            } else if (expectedOperation == null) {
                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                    String.format(OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected operation '%s' is found for path '%s'.",
                        operationType,
                        context.getActualPath()));
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

    private String normalizePath(String path) {
        String s = path;
        if (s == null) {
            s = "/";
        }
        s = s.replaceAll("\\{[^}]*}", "{}");
        while (!Objects.equals(s, s.replaceAll("//", "/"))) {
            s = s.replaceAll("//", "/");
        }
        if (s.length() == 0 || !s.startsWith("/")) {
            s = "/" + s;
        }
        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private Method findMethodByPath(Class<?> serviceClass, String path) {
        path = normalizePath(path);
        Path classPathAnnotation = serviceClass.getAnnotation(Path.class);
        for (Method method : serviceClass.getMethods()) {
            Path pathAnnotation = method.getAnnotation(Path.class);
            if (pathAnnotation != null) {
                String methodPath = (classPathAnnotation != null ? classPathAnnotation.value() : "") + pathAnnotation
                    .value();
                methodPath = normalizePath(methodPath);
                if (Objects.equals(methodPath, path)) {
                    return method;
                }
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
                    if (expectedMediaType == null) {
                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                            String.format(
                                OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected operation '%s' with media type '%s' is found for path '%s'.",
                                context.getOperationType(),
                                entry.getKey(),
                                context.getActualPath()));
                    } else if (actualMediaType == null) {
                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                            String.format(
                                OPEN_API_VALIDATION_MSG_PREFIX + "Expected operation '%s' with media type '%s' is not found for path '%s'.",
                                context.getOperationType(),
                                entry.getKey(),
                                context.getActualPath()));
                    } else {
                        try {
                            context.setActualMediaType(actualMediaType);
                            context.setExpectedMediaType(expectedMediaType);
                            context.setMediaType(entry.getKey());
                            validateRequestBodyInput(context);
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
                            OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected operation '%s' with media type '%s' is found for path '%s'.",
                            context.getOperationType(),
                            entry.getKey(),
                            context.getActualPath()));
                }
            }
        }

        if (actualOperation.getParameters() != null) {
            IOpenMethod openMethod = context.getOpenMethod();
            Method method = context.getMethodMap().get(context.getMethod());
            final String methodName = openMethod != null ? openMethod.getName() : method.getName();
            for (Parameter parameter : actualOperation.getParameters()) {
                Parameter actualParameter = context.getActualOpenAPIResolver().resolve(parameter, Parameter::get$ref);
                boolean found = false;
                if (!CollectionUtils.isEmpty(expectedOperation.getParameters())) {
                    for (Parameter p : expectedOperation.getParameters()) {
                        Parameter expectedParameter = context.getActualOpenAPIResolver().resolve(p, Parameter::get$ref);
                        if (Objects.equals(actualParameter.getIn(), expectedParameter.getIn())) {
                            int index = findParameterIndex(context.getMethod(),
                                actualParameter.getIn(),
                                actualParameter.getName());
                            if ("path".equalsIgnoreCase(actualParameter.getIn())) {
                                String s = extractPathParameterName(context.getExpectedPath(), index);
                                if (Objects.equals(s, expectedParameter.getName())) {
                                    found = true;
                                    validateParameter(context,
                                        openMethod,
                                        methodName,
                                        actualParameter,
                                        expectedParameter,
                                        index);
                                    break;
                                }
                            } else {
                                if (Objects.equals(actualParameter.getName(), expectedParameter.getName())) {
                                    found = true;
                                    validateParameter(context,
                                        openMethod,
                                        methodName,
                                        actualParameter,
                                        expectedParameter,
                                        index);
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!found) {
                    OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                        String.format(
                            OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected %s parameter '%s' is found in method '%s'%s.",
                            actualParameter.getIn(),
                            actualParameter.getName(),
                            methodName,
                            getMethodRelatedPathStringPart(methodName, context.getActualPath())));
                }
            }
            for (Parameter parameter : expectedOperation.getParameters()) {
                Parameter expectedParameter = context.getExpectedOpenAPIResolver()
                    .resolve(parameter, Parameter::get$ref);
                boolean found = false;
                if (!CollectionUtils.isEmpty(actualOperation.getParameters())) {
                    for (Parameter p : actualOperation.getParameters()) {
                        Parameter actualParameter = context.getActualOpenAPIResolver().resolve(p, Parameter::get$ref);
                        if (Objects.equals(actualParameter.getIn(), expectedParameter.getIn())) {
                            int index = findParameterIndex(context.getMethod(),
                                actualParameter.getIn(),
                                actualParameter.getName());
                            if ("path".equalsIgnoreCase(actualParameter.getIn())) {
                                String s = extractPathParameterName(context.getExpectedPath(), index);
                                if (Objects.equals(s, expectedParameter.getName())) {
                                    found = true;
                                    break;
                                }
                            } else {
                                if (Objects.equals(actualParameter.getName(), expectedParameter.getName())) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!found) {
                    OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                        String.format(
                            OPEN_API_VALIDATION_MSG_PREFIX + "Expected %s parameter '%s' is not found in method '%s'%s.",
                            expectedParameter.getIn(),
                            expectedParameter.getName(),
                            methodName,
                            getMethodRelatedPathStringPart(methodName, context.getActualPath())));
                }
            }
        }

        if (expectedOperation.getResponses() != null || actualOperation.getResponses() != null) {
            if (expectedOperation.getResponses() == null) {
                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                    String.format(
                        OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected response is found in operation '%s' for path '%s'.",
                        context.getOperationType(),
                        context.getActualPath()));
            } else if (actualOperation.getResponses() == null) {
                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                    String.format(
                        OPEN_API_VALIDATION_MSG_PREFIX + "Expected response is not found in operation '%s' for path '%s'.",
                        context.getOperationType(),
                        context.getActualPath()));
            } else {
                ApiResponse expectedApiResponse = expectedOperation.getResponses().get("200");
                if (expectedApiResponse == null) {
                    expectedApiResponse = expectedOperation.getResponses().getDefault();
                }
                ApiResponse actualApiResponse = actualOperation.getResponses().get("200");
                if (actualApiResponse == null) {
                    actualApiResponse = actualOperation.getResponses().getDefault();
                }
                expectedApiResponse = context.getExpectedOpenAPIResolver()
                    .resolve(expectedApiResponse, ApiResponse::get$ref);
                actualApiResponse = context.getActualOpenAPIResolver().resolve(actualApiResponse, ApiResponse::get$ref);
                if (expectedApiResponse != null || actualApiResponse != null) {
                    if (expectedApiResponse == null) {
                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                            String.format(
                                OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected response is found in operation '%s' for path '%s'.",
                                context.getOperationType(),
                                context.getActualPath()));
                    } else if (actualApiResponse == null) {
                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                            String.format(
                                OPEN_API_VALIDATION_MSG_PREFIX + "Expected response is found in operation '%s' for path '%s'.",
                                context.getOperationType(),
                                context.getActualPath()));

                    } else {
                        if (expectedApiResponse.getContent() != null) {
                            for (Map.Entry<String, MediaType> entry : expectedApiResponse.getContent().entrySet()) {
                                MediaType expectedMediaType = entry.getValue();
                                MediaType actualMediaType = actualApiResponse.getContent().get(entry.getKey());
                                if (expectedMediaType != null || actualMediaType != null) {
                                    if (actualMediaType == null) {
                                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                                            String.format(
                                                OPEN_API_VALIDATION_MSG_PREFIX + "Expected response is not found in operation '%s' with media type '%s' for path '%s'.",
                                                context.getOperationType(),
                                                entry.getKey(),
                                                context.getActualPath()));
                                    } else if (expectedMediaType == null) {
                                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                                            String.format(
                                                OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected response is found in operation '%s' with media type '%s' for path '%s'.",
                                                context.getOperationType(),
                                                entry.getKey(),
                                                context.getActualPath()));
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
                                            OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected response is found in operation '%s' with media type '%s' for path '%s'.",
                                            context.getOperationType(),
                                            entry.getKey(),
                                            context.getActualPath()));
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
                    OPEN_API_VALIDATION_MSG_PREFIX + "Out-of band callback in operation '%s' for path '%s' is ignored. Callbacks are not supported.",
                    context.getOperationType(),
                    context.getActualPath()));
        }
    }

    private String extractPathParameterName(String path, int index) {
        String s = path;
        int i = 0;
        while (i < index) {
            s = s.substring(s.indexOf("}"));
            i++;
        }
        s = s.substring(s.indexOf("{") + 1);
        s = s.substring(0, s.indexOf("}"));
        return s;
    }

    private void validateParameter(Context context,
            IOpenMethod openMethod,
            String methodName,
            Parameter actualParameter,
            Parameter expectedParameter,
            int index) {
        IOpenClass parameterType = JavaOpenClass.getOpenClass(context.getMethod().getParameterTypes()[index]);
        String parameterName = actualParameter.getName();
        if (openMethod != null) {
            int i = index;
            if (context.isProvideRuntimeContext()) {
                i = i - 1;
            }
            if (i >= 0 && i < openMethod.getSignature().getNumberOfParameters()) {
                parameterType = openMethod.getSignature().getParameterType(i);
                parameterName = openMethod.getSignature().getParameterName(i);
            }
        }
        context.setTypeValidationInProgress(true);
        try {
            validateType(context,
                actualParameter.getSchema(),
                expectedParameter.getSchema(),
                parameterType,
                new HashSet<>());
        } catch (DifferentTypesException e) {
            OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                String.format(
                    OPEN_API_VALIDATION_MSG_PREFIX + "Type '%s' of parameter '%s' in method '%s'%s must be compatible with OpenAPI %s.",
                    parameterType.getDisplayName(INamedThing.REGULAR),
                    parameterName,
                    methodName,
                    getMethodRelatedPathStringPart(methodName, context.getActualPath()),
                    buildOpenApiTypeMessagePart(expectedParameter.getSchema())));
        }
    }

    private int findParameterIndex(Method method, String in, String name) {
        Predicate<Annotation> predicate;
        if ("path".equalsIgnoreCase(in)) {
            predicate = e -> e instanceof PathParam && Objects.equals(((PathParam) e).value(), name);
        } else if ("query".equalsIgnoreCase(in)) {
            predicate = e -> e instanceof QueryParam && Objects.equals(((QueryParam) e).value(), name);
        } else if ("header".equalsIgnoreCase(in)) {
            predicate = e -> e instanceof HeaderParam && Objects.equals(((HeaderParam) e).value(), name);
        } else if ("cookie".equalsIgnoreCase(in)) {
            predicate = e -> e instanceof QueryParam && Objects.equals(((QueryParam) e).value(), name);
        } else {
            throw new IllegalStateException("Parameter type is not resolved");
        }
        int index = 0;
        for (Annotation[] paramAnnotations : method.getParameterAnnotations()) {
            for (Annotation paramAnnotation : paramAnnotations) {
                if (predicate.test(paramAnnotation)) {
                    return index;
                }
            }
            index++;
        }
        throw new IllegalStateException("Failed to resolve parameter index");
    }

    private void validateResponse(Context context) {
        MediaType expectedMediaType = context.getExpectedMediaType();
        MediaType actualMediaType = context.getActualMediaType();
        IOpenMethod openMethod = context.getOpenMethod();
        if (expectedMediaType.getSchema() == null) {
            OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                String.format(
                    OPEN_API_VALIDATION_MSG_PREFIX + "Schema definition for the response in operation '%s' with media type '%s' for path '%s' is not found.",
                    context.getOperationType(),
                    context.getMediaType(),
                    context.getActualPath()));
            return;
        }
        Method method = context.getMethodMap().get(context.getMethod());
        IOpenClass returnType = openMethod != null && method.getReturnType() == openMethod.getType()
            .getInstanceClass() ? openMethod.getType() : JavaOpenClass.getOpenClass(method.getReturnType());
        try {
            context.setTypeValidationInProgress(true);
            try {
                validateType(context,
                    actualMediaType.getSchema(),
                    expectedMediaType.getSchema(),
                    returnType,
                    new HashSet<>());
            } catch (DifferentTypesException e) {
                String methodName = openMethod != null ? openMethod.getName() : method.getName();
                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                    String.format(
                        OPEN_API_VALIDATION_MSG_PREFIX + "Return type of method '%s'%s must be compatible with OpenAPI %s.",
                        methodName,
                        getMethodForPathStringPart(methodName, context.getActualPath()),
                        buildOpenApiTypeMessagePart(expectedMediaType.getSchema())));
            }
        } finally {
            context.setTypeValidationInProgress(false);
        }
    }

    @SuppressWarnings("rawtypes")
    private String buildOpenApiTypeMessagePart(Schema schema) {
        String s = resolveSimplifiedName(schema);
        if (s == null) {
            return "schema";
        }
        int dim = 0;
        StringBuilder arraySuffix = new StringBuilder();
        while (s.endsWith("[]")) {
            s = s.substring(0, s.length() - 2);
            arraySuffix.append("[]");
            dim++;
            schema = ((ArraySchema) schema).getItems();
        }
        String prefix = StringUtils.EMPTY;
        if (dim > 0) {
            prefix = String.format("array%s of ", arraySuffix.toString());
        }
        String type = resolveType(schema);
        String format = schema.getFormat();
        return (dim == 0 && isSimpleJavaType(
            s) ? "type" : "schema") + " '" + prefix + type + (format != null ? "(" + format + ")" : "") + "'";
    }

    private String getMethodForPathStringPart(String methodName, String path) {
        String pathPart = "";
        if (!path.equals("/" + methodName)) {
            pathPart = String.format(" for path '%s'", path);
        }
        return pathPart;
    }

    private String getMethodRelatedPathStringPart(String methodName, String path) {
        String pathPart = "";
        if (!path.equals("/" + methodName)) {
            pathPart = String.format(" related to path '%s'", path);
        }
        return pathPart;
    }

    @SuppressWarnings("rawtypes")
    private void validateRequestBodyInput(Context context) {
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
                    OPEN_API_VALIDATION_MSG_PREFIX + "Failed to resolve a schema definition for operation '%s' with media type '%s' for path '%s'.",
                    context.getOperationType(),
                    context.getMediaType(),
                    context.getActualPath()));
            return;
        }
        Method method = context.getMethodMap().get(context.getMethod());

        Map<String, Schema> allPropertiesOfActualSchema = context.getActualOpenAPIResolver()
            .resolveAllProperties(actualSchema);

        Map<String, Schema> allPropertiesOfExpectedSchema = context.getExpectedOpenAPIResolver()
            .resolveAllProperties(expectedSchema);

        final String methodName = openMethod != null ? openMethod.getName() : method.getName();

        if (method.getParameterCount() > 1 && context.getMethod().getParameterCount() == 1) {
            for (Map.Entry<String, Schema> entry : allPropertiesOfExpectedSchema.entrySet()) {
                Schema<?> actualParameterSchema = allPropertiesOfActualSchema.get(entry.getKey());
                if (actualParameterSchema != null) {
                    // Use openl types instead of java types
                    Pair<String, IOpenClass> parameter = findParameter(context, entry.getKey());
                    validateMethodParameter(context,
                        methodName,
                        entry.getKey(),
                        parameter.getLeft(),
                        parameter.getRight(),
                        actualParameterSchema,
                        entry.getValue());
                } else {
                    OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                        String.format(
                            OPEN_API_VALIDATION_MSG_PREFIX + "Expected parameter for request body schema property '%s' is not found in method '%s'%s.",
                            entry.getKey(),
                            methodName,
                            getMethodRelatedPathStringPart(methodName, context.getActualPath())));
                }
            }
            for (Map.Entry<String, Schema> entry : allPropertiesOfActualSchema.entrySet()) {
                if (allPropertiesOfExpectedSchema.get(entry.getKey()) == null) {
                    Pair<String, IOpenClass> parameter = findParameter(context, entry.getKey());
                    OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                        String.format(
                            OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected parameter '%s' is found in method '%s'%s.",
                            parameter.getLeft(),
                            methodName,
                            getMethodRelatedPathStringPart(methodName, context.getActualPath())));
                }
            }
        } else {
            if (method.getParameterCount() == 1 && isJAXRSBeanParamAnnotationPresented(
                method.getParameterAnnotations()[0])) {
                validateMethodParameter(context,
                    methodName,
                    null,
                    openMethod != null ? openMethod.getSignature().getParameterName(0)
                                       : method.getParameters()[0].getName(),
                    openMethod != null ? openMethod.getSignature().getParameterType(0)
                                       : JavaOpenClass.getOpenClass(method.getParameterTypes()[0]),
                    actualSchema,
                    expectedSchema);
            } else if (method.getParameterCount() > 0) {
                int i = 0;
                for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
                    if (!isJAXRSParameterAnnotationPresented(parameterAnnotations)) {
                        Pair<String, IOpenClass> parameter = findParameter(context, method, i);
                        validateMethodParameter(context,
                            methodName,
                            null,
                            parameter.getLeft(),
                            parameter.getRight(),
                            actualSchema,
                            expectedSchema);
                        return;
                    }
                    i++;
                }
                for (i = 0; i < method.getParameterCount(); i++) {
                    String name = getJAXRSFormParamAnnotation(method.getParameterAnnotations()[i]);
                    if (name != null) {
                        Schema<?> actualParameterSchema = allPropertiesOfActualSchema.get(name);
                        Schema<?> expectedParameterSchema = allPropertiesOfExpectedSchema.get(name);
                        if (expectedParameterSchema != null) {
                            Pair<String, IOpenClass> parameter = findParameter(context, method, i);
                            validateMethodParameter(context,
                                methodName,
                                name,
                                parameter.getLeft(),
                                parameter.getRight(),
                                actualParameterSchema,
                                expectedParameterSchema);
                        } else {
                            OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                                String.format(
                                    OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected parameter for request body schema property '%s' is found in method '%s'%s.",
                                    name,
                                    methodName,
                                    getMethodRelatedPathStringPart(methodName, context.getActualPath())));
                        }
                    }
                }
                for (Map.Entry<String, Schema> entry : allPropertiesOfExpectedSchema.entrySet()) {
                    if (allPropertiesOfActualSchema.get(entry.getKey()) == null) {
                        OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                            String.format(
                                OPEN_API_VALIDATION_MSG_PREFIX + "Expected parameter for request body schema property '%s' is not found in method '%s'%s.",
                                entry.getKey(),
                                methodName,
                                getMethodRelatedPathStringPart(methodName, context.getActualPath())));
                    }
                }
            }
        }
    }

    private Pair<String, IOpenClass> findParameter(Context context, Method method, int i) {
        int index = i;
        if (context.isProvideRuntimeContext()) {
            index = index - 1;
        }
        String parameterName = method.getParameters()[i].getName();
        IOpenClass parameterType = JavaOpenClass.getOpenClass(method.getParameterTypes()[i]);
        if (context.getOpenMethod() != null && index >= 0 && index < context.getOpenMethod()
            .getSignature()
            .getNumberOfParameters()) {
            parameterName = context.getOpenMethod().getSignature().getParameterName(index);
            parameterType = context.getOpenMethod().getSignature().getParameterType(index);
        }
        return Pair.of(parameterName, parameterType);
    }

    private Pair<String, IOpenClass> findParameter(Context context, String propertyName) {
        JavaOpenField javaOpenField = (JavaOpenField) context.getOpenClassPropertiesResolver()
            .findFieldByPropertyName(JavaOpenClass.getOpenClass(context.getMethod().getParameterTypes()[0]),
                propertyName);
        String parameterName = javaOpenField.getName();
        IOpenClass parameterType = javaOpenField.getType();
        if (context.getOpenMethod() != null) {
            ParameterIndex parameterIndex = javaOpenField.getJavaField().getAnnotation(ParameterIndex.class);
            int index = parameterIndex.value();
            if (context.isProvideRuntimeContext()) {
                index = index - 1;
            }
            if (index >= 0 && index < context.getOpenMethod().getSignature().getNumberOfParameters()) {
                parameterName = context.getOpenMethod().getSignature().getParameterName(index);
                parameterType = context.getOpenMethod().getSignature().getParameterType(index);
            }
        }
        return Pair.of(parameterName, parameterType);
    }

    private void validateMethodParameter(Context context,
            String methodName,
            String parameterPropertyName,
            String parameterName,
            IOpenClass parameterType,
            Schema<?> actualParameterSchema,
            Schema<?> expectedParameterSchema) {
        if (expectedParameterSchema == null) {
            OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                String.format(
                    OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected parameter for schema property%s is found in method '%s'%s.",
                    parameterPropertyName != null ? String.format(" '%s'", parameterPropertyName) : "",
                    methodName,
                    getMethodRelatedPathStringPart(methodName, context.getActualPath())));
        } else {
            try {
                context.setTypeValidationInProgress(true);
                try {
                    validateType(context,
                        actualParameterSchema,
                        expectedParameterSchema,
                        parameterType,
                        new HashSet<>());
                } catch (DifferentTypesException e) {
                    OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                        String.format(
                            OPEN_API_VALIDATION_MSG_PREFIX + "Type '%s' of parameter '%s' in method '%s'%s must be compatible with OpenAPI %s.",
                            parameterType.getDisplayName(INamedThing.REGULAR),
                            parameterName,
                            methodName,
                            getMethodRelatedPathStringPart(methodName, context.getActualPath()),
                            buildOpenApiTypeMessagePart(expectedParameterSchema)));
                }
            } finally {
                context.setTypeValidationInProgress(false);
            }
        }
    }

    private boolean isJAXRSParameterAnnotationPresented(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof PathParam || annotation instanceof QueryParam || annotation instanceof CookieParam || annotation instanceof FormParam || annotation instanceof BeanParam || annotation instanceof HeaderParam || annotation instanceof MatrixParam) {
                return true;
            }
        }
        return false;
    }

    private String getJAXRSFormParamAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof FormParam) {
                FormParam formParam = (FormParam) annotation;
                return formParam.value();
            }
        }
        return null;
    }

    private boolean isJAXRSBeanParamAnnotationPresented(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof BeanParam) {
                return true;
            }
        }
        return false;
    }

    private static final List<String> ORDER_TYPES1 = Arrays.asList("Integer", "Long", "BigInteger", "BigDecimal");
    private static final List<String> ORDER_TYPES2 = Arrays.asList("Integer", "Long", "Float", "Double", "BigDecimal");

    private boolean isCompatibleSimpleTypes(String actualType, String expectedType) {
        int actualIndex = ORDER_TYPES1.indexOf(actualType);
        int expectedIndex = ORDER_TYPES1.indexOf(expectedType);
        if (actualIndex >= 0 && expectedIndex >= 0 && actualIndex <= expectedIndex) {
            return true;
        }
        actualIndex = ORDER_TYPES2.indexOf(actualType);
        expectedIndex = ORDER_TYPES2.indexOf(expectedType);
        if (actualIndex >= 0 && expectedIndex >= 0 && actualIndex <= expectedIndex) {
            return true;
        }
        return Objects.equals(actualType, expectedType);
    }

    private boolean isSimpleJavaType(String type) {
        return "String".equals(type) || "Float".equals(type) || "Double".equals(type) || "Integer"
            .equals(type) || "Long".equals(type) || "Boolean"
                .equals(type) || "Date".equals(type) || "BigDecimal".equals(type) || "BigInteger".equals(type);
    }

    private String resolveType(Schema<?> schema) {
        if (schema.get$ref() != null) {
            return RefUtils.computeDefinitionName(schema.get$ref());
        }
        return schema.getType();
    }

    private String resolveSimplifiedName(Schema<?> schema) {
        if (schema == null) {
            return "void";
        }
        if (schema.get$ref() != null) {
            return RefUtils.computeDefinitionName(schema.get$ref());
        }
        if ("object".equals(schema.getType())) {
            return "object";
        } else if ("string".equals(schema.getType())) {
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
                return "BigDecimal";
            }
        } else if ("integer".equals(schema.getType())) {
            if ("int32".equals(schema.getFormat())) {
                return "Integer";
            } else if ("int64".equals(schema.getFormat())) {
                return "Long";
            } else {
                return "BigInteger";
            }
        } else if ("boolean".equals(schema.getType())) {
            return "Boolean";
        } else if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            String type = resolveSimplifiedName(arraySchema.getItems());
            return type != null ? type + "[]" : null;
        }
        return null;
    }

    interface Function {
        void run();
    }

    private IOpenClass getSuperClass(IOpenClass openClass) {
        for (IOpenClass superClass : openClass.superClasses()) {
            if (!superClass.isInterface()) {
                return superClass;
            }
        }
        return null;
    }

    private static class Key {
        private final IOpenClass openClass;
        private final IOpenClass openFieldType;
        private final String expectedSchemaRef;

        public Key(IOpenClass openClass, IOpenClass openFieldType, String expectedSchemaRef) {
            this.openClass = openClass;
            this.openFieldType = openFieldType;
            this.expectedSchemaRef = expectedSchemaRef;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            Key key = (Key) o;
            if (!Objects.equals(openClass, key.openClass))
                return false;
            if (!Objects.equals(openFieldType, key.openFieldType))
                return false;
            return Objects.equals(expectedSchemaRef, key.expectedSchemaRef);
        }

        @Override
        public int hashCode() {
            int result = openClass != null ? openClass.hashCode() : 0;
            result = 31 * result + (openFieldType != null ? openFieldType.hashCode() : 0);
            result = 31 * result + (expectedSchemaRef != null ? expectedSchemaRef.hashCode() : 0);
            return result;
        }
    }

    @SuppressWarnings("rawtypes")
    private void validateType(Context context,
            Schema<?> actualSchema,
            Schema<?> expectedSchema,
            IOpenClass openClass,
            Set<Key> validatedSchemas) throws DifferentTypesException {
        IOpenClass oldType = context.getType();
        try {
            Schema<?> resolvedActualSchema = context.getActualOpenAPIResolver().resolve(actualSchema, Schema::get$ref);
            if (resolvedActualSchema != null) {
                Schema<?> resolvedExpectedSchema = context.getExpectedOpenAPIResolver()
                    .resolve(expectedSchema, Schema::get$ref);
                if (openClass.isArray() && resolvedActualSchema instanceof ArraySchema) {
                    if (resolvedExpectedSchema instanceof ArraySchema) {
                        validateType(context,
                            ((ArraySchema) resolvedActualSchema).getItems(),
                            ((ArraySchema) resolvedExpectedSchema).getItems(),
                            openClass.getComponentClass(),
                            validatedSchemas);
                        return;
                    } else {
                        throw new DifferentTypesException();
                    }
                }
                if (resolvedExpectedSchema instanceof ArraySchema && (!openClass
                    .isArray() || resolvedActualSchema instanceof ArraySchema)) {
                    throw new DifferentTypesException();
                }

                String resolvedActualSchemaSimplifiedName = resolveSimplifiedName(resolvedActualSchema);
                String resolvedExpectedSchemaSimplifiedName = resolveSimplifiedName(resolvedExpectedSchema);
                if (isSimpleJavaType(resolvedActualSchemaSimplifiedName) || isSimpleJavaType(
                    resolvedExpectedSchemaSimplifiedName)) {
                    if (isSimpleJavaType(resolvedActualSchemaSimplifiedName) && isSimpleJavaType(
                        resolvedExpectedSchemaSimplifiedName)) {
                        if (!isCompatibleSimpleTypes(resolvedActualSchemaSimplifiedName,
                            resolvedExpectedSchemaSimplifiedName)) {
                            throw new DifferentTypesException();
                        }
                    } else {
                        throw new DifferentTypesException();
                    }
                }

                context.setType(openClass);
                Map<String, Schema> propertiesOfExpectedSchema = null;
                Map<String, Schema> propertiesOfActualSchema = null;
                boolean parentPresentedInBothSchemas = false;

                if (resolvedExpectedSchema instanceof ComposedSchema && resolvedActualSchema instanceof ComposedSchema) {
                    ComposedSchema actualComposedSchema = (ComposedSchema) resolvedActualSchema;
                    ComposedSchema expectedComposedSchema = (ComposedSchema) resolvedExpectedSchema;
                    if (isParentPresented(actualComposedSchema) && isParentPresented(expectedComposedSchema)) {
                        IOpenClass superClass = getSuperClass(openClass);
                        if (superClass != null) {
                            try {
                                validateType(context,
                                    extractParentSchema(actualComposedSchema),
                                    extractParentSchema(expectedComposedSchema),
                                    superClass,
                                    validatedSchemas);
                            } catch (DifferentTypesException e) {
                                String schemaToString = schemaToString(context,
                                    extractParentSchema(expectedComposedSchema));
                                OpenApiProjectValidatorMessagesUtils.addTypeError(context,
                                    String.format(
                                        OPEN_API_VALIDATION_MSG_PREFIX + "Parent '%s' of type '%s' mismatches to declared schema%s",
                                        superClass.getDisplayName(INamedThing.REGULAR),
                                        openClass.getDisplayName(INamedThing.REGULAR),
                                        schemaToString == null ? "." : ":\n" + schemaToString));
                            }
                            propertiesOfExpectedSchema = extractObjectSchema(expectedComposedSchema).getProperties();
                            propertiesOfActualSchema = extractObjectSchema(actualComposedSchema).getProperties();
                            if (propertiesOfActualSchema == null) {
                                propertiesOfActualSchema = Collections.emptyMap();
                            }
                            if (propertiesOfExpectedSchema == null) {
                                propertiesOfExpectedSchema = Collections.emptyMap();
                            }
                            parentPresentedInBothSchemas = true;
                        }
                    }
                }
                if (!parentPresentedInBothSchemas) {
                    context.setType(openClass);
                    propertiesOfExpectedSchema = context.getExpectedOpenAPIResolver()
                        .resolveAllProperties(expectedSchema);
                    propertiesOfActualSchema = context.getActualOpenAPIResolver()
                        .resolveAllProperties(resolvedActualSchema);
                }

                List<Function> wrongFields = new ArrayList<>();
                int countOfValidFields = 0;
                for (Map.Entry<String, Schema> entry : propertiesOfExpectedSchema.entrySet()) {
                    Schema<?> fieldActualSchema = propertiesOfActualSchema.get(entry.getKey());
                    if (fieldActualSchema == null) {
                        if (context.getSpreadsheetMethodResolver()
                            .resolve(openClass) == null || openClass instanceof SpreadsheetResultOpenClass) {
                            wrongFields.add(() -> OpenApiProjectValidatorMessagesUtils.addTypeError(context,
                                String.format(
                                    OPEN_API_VALIDATION_MSG_PREFIX + "Expected non transient field for schema property '%s' is not found in type '%s'.",
                                    entry.getKey(),
                                    openClass.getDisplayName(INamedThing.REGULAR))));
                        } else {
                            wrongFields.add(() -> OpenApiProjectValidatorMessagesUtils.addTypeError(context,
                                String.format(
                                    OPEN_API_VALIDATION_MSG_PREFIX + "Expected non transient cell for schema property '%s' is not found.",
                                    entry.getKey())));
                        }
                    } else {
                        IOpenField openField = context.getOpenClassPropertiesResolver()
                            .findFieldByPropertyName(openClass, entry.getKey());
                        if (openField != null) {
                            BiPredicate<Schema, IOpenField> isIncompatibleTypesPredicate = (e1, f) -> {
                                try {
                                    if (expectedSchema.get$ref() != null) {
                                        Key key = new Key(openClass, openField.getType(), expectedSchema.get$ref());
                                        if (!validatedSchemas.contains(key)) {
                                            validatedSchemas.add(key);
                                            try {
                                                validateType(context,
                                                    e1,
                                                    entry.getValue(),
                                                    f.getType(),
                                                    validatedSchemas);
                                            } finally {
                                                validatedSchemas.remove(key);
                                            }
                                        }
                                    } else {
                                        validateType(context, e1, entry.getValue(), f.getType(), validatedSchemas);
                                    }
                                    return false;
                                } catch (DifferentTypesException e2) {
                                    return true;
                                }
                            };
                            if (isIncompatibleTypesPredicate.test(fieldActualSchema, openField)) {
                                final String stepName = context.getSpreadsheetMethodResolver()
                                    .resolveStepName(context.getType(), openField);
                                wrongFields.add(() -> {
                                    try {
                                        context.setField(openField);
                                        context.setIsIncompatibleTypesPredicate(isIncompatibleTypesPredicate);
                                        String actualSchemaMessagePartString = buildOpenApiTypeMessagePart(
                                            fieldActualSchema);
                                        if (Objects.equals("schema", actualSchemaMessagePartString)) {
                                            actualSchemaMessagePartString = StringUtils.EMPTY;
                                        } else {
                                            actualSchemaMessagePartString = String.format(
                                                " that incompatible with actual %s",
                                                actualSchemaMessagePartString);
                                        }
                                        if (stepName == null) {
                                            OpenApiProjectValidatorMessagesUtils.addTypeError(context,
                                                String.format(
                                                    OPEN_API_VALIDATION_MSG_PREFIX + "Type of field '%s' in type '%s' must be compatible with OpenAPI %s%s.",
                                                    openField.getName(),
                                                    openClass.getDisplayName(INamedThing.REGULAR),
                                                    buildOpenApiTypeMessagePart(entry.getValue()),
                                                    actualSchemaMessagePartString));
                                        } else {
                                            OpenApiProjectValidatorMessagesUtils.addTypeError(context,
                                                String.format(
                                                    OPEN_API_VALIDATION_MSG_PREFIX + "Type of cell '%s' must be compatible with OpenAPI %s%s.",
                                                    stepName,
                                                    buildOpenApiTypeMessagePart(entry.getValue()),
                                                    actualSchemaMessagePartString));
                                        }
                                    } finally {
                                        context.setIsIncompatibleTypesPredicate(null);
                                        context.setField(null);
                                    }
                                });
                            } else {
                                countOfValidFields++;
                            }
                        }
                    }
                }
                for (Map.Entry<String, Schema> entry : propertiesOfActualSchema.entrySet()) {
                    Schema<?> fieldExpectedSchema = propertiesOfExpectedSchema.get(entry.getKey());
                    if (fieldExpectedSchema == null) {
                        IOpenField openField = context.getOpenClassPropertiesResolver()
                            .findFieldByPropertyName(openClass, entry.getKey());
                        if (openField != null) {
                            final String stepName = context.getSpreadsheetMethodResolver()
                                .resolveStepName(context.getType(), openField);
                            wrongFields.add(() -> {
                                try {
                                    context.setField(openField);
                                    if (stepName == null) {
                                        OpenApiProjectValidatorMessagesUtils.addTypeError(context,
                                            String.format(
                                                OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected field '%s' is found in type '%s'.",
                                                openField.getName(),
                                                openClass.getDisplayName(INamedThing.REGULAR)));
                                    } else {
                                        OpenApiProjectValidatorMessagesUtils.addTypeError(context,
                                            String.format(
                                                OPEN_API_VALIDATION_MSG_PREFIX + "Unexpected schema property '%s' related to cell '%s' is found.",
                                                entry.getKey(),
                                                stepName));
                                    }
                                } finally {
                                    context.setField(null);
                                }
                            });
                        }
                    }
                }

                if (countOfValidFields == 0 && !wrongFields.isEmpty()) {
                    if (expectedSchema.get$ref() == null || actualSchema.get$ref() == null || !Objects.equals(
                        RefUtils.computeDefinitionName(expectedSchema.get$ref()),
                        RefUtils.computeDefinitionName(actualSchema.get$ref()))) {
                        throw new DifferentTypesException();
                    }
                }
                wrongFields.forEach(Function::run);
            }
        } finally {
            context.setType(oldType);
        }
    }

    private String schemaToString(Context context, Schema<?> extractParentSchema) {
        if (extractParentSchema == null) {
            return null;
        }
        ObjectMapper objectMapper;
        if (context.isYaml()) {
            objectMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        } else {
            objectMapper = new ObjectMapper();
        }
        objectMapper.deactivateDefaultTyping();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return objectMapper.writeValueAsString(extractParentSchema);
        } catch (JsonProcessingException ignore) {
            return null;
        }
    }

    private boolean isParentPresented(ComposedSchema expectedComposedSchema) {
        if (expectedComposedSchema.getAllOf() != null) {
            int i = 0;
            for (Schema<?> schema : expectedComposedSchema.getAllOf()) {
                if (schema instanceof ObjectSchema) {
                    i++;
                }
            }
            return expectedComposedSchema.getAllOf().size() == 2 && i == 1;
        }
        return false;
    }

    private Schema<?> extractObjectSchema(ComposedSchema expectedComposedSchema) {
        if (expectedComposedSchema.getAllOf() != null) {
            for (Schema<?> schema : expectedComposedSchema.getAllOf()) {
                if (schema instanceof ObjectSchema) {
                    return schema;
                }
            }
        }
        throw new IllegalStateException("Object schema is not found");
    }

    private Schema<?> extractParentSchema(ComposedSchema expectedComposedSchema) {
        if (expectedComposedSchema.getAllOf() != null) {
            for (Schema<?> schema : expectedComposedSchema.getAllOf()) {
                if (!(schema instanceof ObjectSchema)) {
                    return schema;
                }
            }
        }
        throw new IllegalStateException("Parent schema is not found");
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
