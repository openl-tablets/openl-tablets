package org.open.rules.project.validation.openapi;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.Path;

import org.apache.commons.jxpath.CompiledExpression;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang3.tuple.Pair;
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
import org.openl.rules.variation.VariationsPack;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.util.RefUtils;

public class OpenApiProjectValidator extends AbstractServiceInterfaceProjectValidator {
    private final Logger log = LoggerFactory.getLogger(OpenApiProjectValidator.class);

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
        OpenAPI actualOpenAPI = reader.read(enhancedServiceClass);
        context.setActualOpenAPI(actualOpenAPI);
        context.setActualOpenAPIJXPathContext(JXPathContext.newContext(actualOpenAPI));
        validateOpenAPI(context);
        return validatedCompiledOpenClass;
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
        for (Map.Entry<String, PathItem> entry : context.getExpectedOpenAPI().getPaths().entrySet()) {
            PathItem expectedPathItem = entry.getValue();
            try {
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
                    context.setMethod(method);
                    context.setOpenMethod(openMethod);
                    validatePathItem(context);
                } else {
                    addMethodError(context,
                        String.format(
                            "The method is not found for the path item '%s', but it is specified in the OpenAPI file.",
                            context.getPath()));
                }
            } finally {
                context.setOpenMethod(null);
                context.setActualPathItem(null);
                context.setExpectedPathItem(null);
                context.setPath(null);
                context.setMethod(null);
            }
        }
        for (Map.Entry<String, PathItem> entry : context.getActualOpenAPI().getPaths().entrySet()) {
            PathItem expectedPathItem = context.getExpectedOpenAPI().getPaths().get(entry.getKey());
            if (expectedPathItem == null) {
                addMethodError(context,
                    String.format(
                        "Unexpected method for the path item '%s' is found, but it is not specified in the OpenAPI file.",
                        entry.getKey()));
            }
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

    private void addTypeError(Context context, IOpenClass openClass, String summary) {
        // TODO Remove duplicates, add error only for the current project
        if (openClass instanceof DatatypeOpenClass) {
            DatatypeOpenClass datatypeOpenClass = (DatatypeOpenClass) openClass;
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
                OpenLMessage openLMessage = OpenLMessagesUtils.newWarnMessage(summary, datatypeOpenClass.getTableSyntaxNode());
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
                        "The operation '%s' for the path item '%s' is found, but is not specified in the OpenAPI file.",
                        operationType,
                        context.getPath()));
            } else if (expectedOperation == null) {
                addMethodError(context,
                    String.format(
                        "The operation '%s' is not found for the path item '%s' that is specified in the OpenAPI file.",
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

        RequestBody actualRequestBody = actualOperation.getRequestBody();
        RequestBody expectedRequestBody = expectedOperation.getRequestBody();

        for (Map.Entry<String, MediaType> entry : expectedRequestBody.getContent().entrySet()) {
            MediaType expectedMediaType = entry.getValue();
            MediaType actualMediaType = actualRequestBody.getContent().get(entry.getKey());
            if (expectedMediaType != null || actualMediaType != null) {
                if (actualMediaType == null) {
                    addMethodError(context,
                        String.format(
                            "The operation '%s' and media type '%s' for the path item '%s' is found, but it is not specified in the OpenAPI file.",
                            context.getOperationType(),
                            entry.getKey(),
                            context.getPath()));
                } else if (expectedMediaType == null) {
                    addMethodError(context,
                        String.format(
                            "The operation '%s' and media type '%s' is not found for the path item '%s', but it is specified in the OpenAPI file.",
                            context.getOperationType(),
                            entry.getKey(),
                            context.getPath()));
                } else {
                    try {
                        context.setActualMediaType(actualMediaType);
                        context.setExpectedMediaType(expectedMediaType);
                        validateMediaType(context);
                    } finally {
                        context.setActualMediaType(null);
                        context.setExpectedMediaType(null);
                    }
                }
            }
        }
        for (Map.Entry<String, MediaType> entry : actualRequestBody.getContent().entrySet()) {
            MediaType expectedMediaType = expectedRequestBody.getContent().get(entry.getKey());
            if (expectedMediaType == null) {
                addMethodError(context,
                    String.format(
                        "The operation '%s' and media type '%s' for the path item '%s' is not specified in the OpenAPI file.",
                        context.getOperationType(),
                        entry.getKey(),
                        context.getPath()));
            }
        }
    }

    private void validateMediaType(Context context) {
        MediaType expectedMediaType = context.getExpectedMediaType();
        MediaType actualMediaType = context.getActualMediaType();
        IOpenMethod openMethod = context.getOpenMethod();
        Schema<?> expectedSchema = resolveSchema(context.getExpectedOpenAPIJXPathContext(),
            expectedMediaType.getSchema());
        Schema<?> actualSchema = resolveSchema(context.getActualOpenAPIJXPathContext(), actualMediaType.getSchema());
        Method method = context.getMethodMap().get(context.getMethod());
        Schema<?>[] parameterSchemas = new Schema[method.getParameterCount()];
        String[] parameterNames = null;
        if (method.getParameterCount() > 1) {
            parameterNames = MethodUtils.getParameterNames(
                isResolveMethodParameterNames() ? context.getOpenClass() : null,
                method,
                context.isProvideRuntimeContext(),
                context.isProvideRuntimeContext());
            int i = 0;
            for (String parameterName : parameterNames) {
                parameterSchemas[i++] = resolveSchema(context.getActualOpenAPIJXPathContext(),
                    actualSchema.getProperties().get(parameterName));
            }
        } else {
            parameterSchemas[0] = actualSchema;
        }
        int i = 0;
        for (Schema<?> parameterSchema : parameterSchemas) {
            Class<?> parameterType = method.getParameterTypes()[i];
            Schema<?> expectedParameterSchema = parameterNames == null ? expectedSchema
                                                                       : expectedSchema
                                                                           .getProperties() != null ? expectedSchema
                                                                               .getProperties()
                                                                               .get(parameterNames[i]) : null;
            if (parameterNames != null && expectedParameterSchema == null) {
                addMethodError(context,
                    String.format("The parameter '%s' for the method '%s' is not specified in the OpenAPI file.",
                        parameterNames[i],
                        method.getName()));
                continue;
            }
            IOpenClass parameterOpenClass = null;
            if (!(context
                .isProvideVariations() && VariationsPack.class == parameterType && i == parameterSchemas.length - 1)) {
                if (context.isProvideRuntimeContext()) {
                    if (i > 0) {
                        parameterOpenClass = openMethod.getSignature().getParameterType(i - 1);
                    }
                } else {
                    parameterOpenClass = openMethod.getSignature().getParameterType(i);
                }
            }
            if (parameterOpenClass == null) {
                parameterOpenClass = JavaOpenClass.getOpenClass(parameterType);
            }

            if (expectedParameterSchema.get$ref() != null && parameterSchema.get$ref() != null) {
                String expectedParameterDefinitionName = RefUtils
                    .computeDefinitionName(expectedParameterSchema.get$ref());
                String parameterDefinitionName = RefUtils.computeDefinitionName(parameterSchema.get$ref());
                if (!Objects.equals(expectedParameterDefinitionName, parameterDefinitionName)) {
                    addMethodError(context,
                        String.format(
                            "The parameter type '%s' for the method '%s' is not matched to the type '%s' specified in the OpenAPI file.",
                            parameterOpenClass.getDisplayName(INamedThing.REGULAR),
                            method.getName(),
                            expectedParameterDefinitionName));
                    continue;
                }
            }
            validateType(context, parameterSchema, expectedParameterSchema, parameterOpenClass, new HashSet<>());
            i++;
        }
    }

    private Schema<?> resolveSchema(JXPathContext jxPathContext, Schema<?> schema) {
        if (schema != null && schema.get$ref() != null) {
            return resolveSchema(jxPathContext, schema.get$ref());
        }
        return schema;
    }

    private Schema<?> resolveSchema(JXPathContext jxPathContext, String ref) {
        ref = ref.substring(1);
        CompiledExpression compiledExpression = JXPathContext.compile(ref);
        return (Schema<?>) compiledExpression.createPath(jxPathContext).getValue();
    }

    private Pair<String, Class<?>> resolveType(Schema<?> schema) {
        if (schema.get$ref() != null) {
            return Pair.of(RefUtils.computeDefinitionName(schema.get$ref()), null);
        }
        if ("string".equals(schema.getType())) {
            return Pair.of("String", String.class);
        } else if ("number".equals(schema.getType())) {
            if ("float".equals(schema.getFormat())) {
                return Pair.of("Float", Float.class);
            } else if ("double".equals(schema.getFormat())) {
                return Pair.of("Double", Double.class);
            } else {
                return Pair.of("Double", Double.class);
            }
        } else if ("integer".equals(schema.getType())) {
            if ("int32".equals(schema.getFormat())) {
                return Pair.of("Integer", Integer.class);
            } else if ("int64".equals(schema.getFormat())) {
                return Pair.of("Long", Long.class);
            } else {
                return Pair.of("Long", Long.class);
            }
        } else if ("boolean".equals(schema.getType())) {
            return Pair.of("Boolean", Boolean.class);
        } else if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            Pair<String, Class<?>> t = resolveType(arraySchema.getItems());
            return Pair.of(t.getKey() + "[]",
                t.getRight() != null ? Array.newInstance(t.getValue(), 0).getClass() : null);
        }
        return Pair.of("Object", Object.class);
    }

    @SuppressWarnings("rawtypes")
    private void validateType(Context context,
            Schema<?> actualSchema,
            Schema<?> expectedSchema,
            IOpenClass openClass,
            Set<Schema<?>> validatedSchemas) {
        Schema<?> resolvedActualSchema = resolveSchema(context.getActualOpenAPIJXPathContext(), actualSchema);
        if (resolvedActualSchema == null) {
            throw new IllegalStateException("Failed to resolve a reference for the generated schema");
        } else {
            Schema<?> resolvedExpectedSchema = resolveSchema(context.getExpectedOpenAPIJXPathContext(), expectedSchema);
            if (resolvedExpectedSchema != null) {
                validatedSchemas.add(resolvedExpectedSchema);
            } else {
                addError(context,
                    String.format("Wrong reference '%s' is used in the OpenAPI file.", expectedSchema.get$ref()));
                return;
            }
            if (resolvedExpectedSchema.getProperties() != null) {
                for (Map.Entry<String, Schema> entry : resolvedExpectedSchema.getProperties().entrySet()) {
                    IOpenField openField = openClass.getField(entry.getKey());
                    if (openField == null) {
                        addTypeError(context,
                            openClass,
                            String.format(
                                "The field '%s' is not defined in the type '%s', but it is specified in the OpenAPI file.",
                                entry.getKey(),
                                openClass.getDisplayName(INamedThing.REGULAR)));
                    } else {
                        Schema<?> fieldActualSchema = resolvedActualSchema
                            .getProperties() != null ? resolvedActualSchema.getProperties().get(entry.getKey()) : null;
                        if (fieldActualSchema == null) {
                            throw new IllegalStateException("Failed to resolve a reference for the generated schema");
                        } else {
                            Pair<String, Class<?>> fieldActualResolverType = resolveType(fieldActualSchema);
                            Pair<String, Class<?>> fieldExpectedResolverType = resolveType(entry.getValue());
                            if (!Objects.equals(fieldActualResolverType.getKey(), fieldExpectedResolverType.getKey())) {
                                addTypeError(context,
                                    openClass,
                                    String.format(
                                        "The field '%s' with type '%s' is defined in the type '%s', but the type is mismatched to the type '%s' specified in the OpenAPI file.",
                                        entry.getKey(),
                                        fieldActualResolverType.getKey(),
                                        openClass.getDisplayName(INamedThing.REGULAR),
                                        fieldExpectedResolverType.getKey()));
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
                if (resolvedActualSchema.getProperties() != null) {
                    for (Map.Entry<String, Schema> entry : resolvedActualSchema.getProperties().entrySet()) {
                        Schema<?> fieldExpectedSchema = resolvedExpectedSchema
                            .getProperties() != null ? resolvedExpectedSchema.getProperties().get(entry.getKey())
                                                     : null;
                        if (fieldExpectedSchema == null) {
                            addTypeWarning(context,
                                openClass,
                                String.format(
                                    "The field '%s' is defined in the type '%s', but it is not specified in the OpenAPI file.",
                                    entry.getKey(),
                                    openClass.getDisplayName(INamedThing.REGULAR)));
                        }
                    }
                }
            }
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
