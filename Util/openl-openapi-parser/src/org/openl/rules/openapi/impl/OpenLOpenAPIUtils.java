package org.openl.rules.openapi.impl;

import static org.openl.rules.openapi.impl.OpenAPITypeUtils.DEFAULT_RUNTIME_CONTEXT;
import static org.openl.rules.openapi.impl.OpenAPITypeUtils.RUNTIME_CONTEXT_TYPE;
import static org.openl.rules.openapi.impl.OpenAPITypeUtils.SCHEMAS_LINK;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.jxpath.CompiledExpression;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang3.tuple.Pair;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.ParameterModel;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.ParseOptions;

public class OpenLOpenAPIUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenLOpenAPIUtils.class);
    public static final String APPLICATION_JSON = "application/json";
    public static final String TEXT_PLAIN = "text/plain";

    public static final byte MAX_PARAMETERS_COUNT = 7;
    public static final int MIN_PARAMETERS_COUNT = 1;

    @SuppressWarnings("unchecked")
    public static <T> T resolve(JXPathContext jxPathContext, T obj, Function<T, String> getRefFuc) {
        if (obj != null && getRefFuc.apply(obj) != null) {
            return resolve(jxPathContext, (T) resolveByRef(jxPathContext, getRefFuc.apply(obj)), getRefFuc);
        }
        return obj;
    }

    public static Object resolveByRef(JXPathContext jxPathContext, String ref) {
        ref = ref.substring(1);
        CompiledExpression compiledExpression = JXPathContext.compile(ref);
        return compiledExpression.createPath(jxPathContext).getValue();
    }

    public static ParseOptions getParseOptions() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        return options;
    }

    public static Set<String> getUnusedSchemaRefs(OpenAPI openAPI, Iterable<String> usedRefs) {
        Set<String> schemaNames = getSchemas(openAPI).keySet()
            .stream()
            .map(name -> SCHEMAS_LINK + name)
            .collect(Collectors.toSet());
        for (String usedRef : usedRefs) {
            schemaNames.remove(usedRef);
        }
        return schemaNames;
    }

    public static Schema<?> getUsedSchemaInResponse(JXPathContext jxPathContext, PathItem pathItem) {
        Schema<?> type = null;
        Pair<Operation, PathItem.HttpMethod> operationWithMethod = OpenLOpenAPIUtils.getOperation(pathItem);
        if (operationWithMethod != null) {
            Operation operation = operationWithMethod.getKey();
            ApiResponses responses = operation.getResponses();
            if (responses != null) {
                ApiResponse response = getResponse(jxPathContext, responses);
                if (response != null && CollectionUtils.isNotEmpty(response.getContent())) {
                    MediaTypeInfo mediaType = OpenLOpenAPIUtils.getMediaType(response.getContent());
                    if (mediaType != null) {
                        Schema<?> mediaTypeSchema = mediaType.getContent().getSchema();
                        if (mediaTypeSchema != null) {
                            type = mediaTypeSchema;
                        }
                    }
                }

            }
        }
        return type;
    }

    public static Pair<Operation, PathItem.HttpMethod> getOperation(PathItem path) {
        Operation result;
        PathItem.HttpMethod method = PathItem.HttpMethod.GET;
        Map<PathItem.HttpMethod, Operation> operationsMap = path.readOperationsMap();
        if (CollectionUtils.isEmpty(operationsMap)) {
            return null;
        }
        if (operationsMap.containsKey(PathItem.HttpMethod.GET)) {
            result = operationsMap.get(PathItem.HttpMethod.GET);
        } else if (operationsMap.containsKey(PathItem.HttpMethod.POST)) {
            result = operationsMap.get(PathItem.HttpMethod.POST);
        } else {
            Map.Entry<PathItem.HttpMethod, Operation> firstOperation = operationsMap.entrySet().iterator().next();
            result = firstOperation.getValue();
            method = firstOperation.getKey();
        }
        return Pair.of(result, method);
    }

    public static ApiResponse getResponse(JXPathContext jxPathContext, ApiResponses apiResponses) {
        if (CollectionUtils.isEmpty(apiResponses)) {
            return null;
        }
        ApiResponse successResponse = apiResponses.get("200");
        ApiResponse defaultResponse = apiResponses.getDefault();
        ApiResponse result;
        if (successResponse != null) {
            result = successResponse;
        } else if (defaultResponse != null) {
            result = defaultResponse;
        } else {
            result = apiResponses.values().iterator().next();
        }
        return resolve(jxPathContext, result, ApiResponse::get$ref);
    }

    public static MediaTypeInfo getMediaType(Content content) {
        Set<String> mediaTypes = content.keySet();
        if (mediaTypes.contains(APPLICATION_JSON)) {
            return new MediaTypeInfo(content.get(APPLICATION_JSON), APPLICATION_JSON);
        } else if (mediaTypes.contains(TEXT_PLAIN)) {
            return new MediaTypeInfo(content.get(TEXT_PLAIN), TEXT_PLAIN);
        } else {
            Optional<Map.Entry<String, MediaType>> mediaType = content.entrySet().stream().findFirst();
            if (mediaType.isPresent()) {
                Map.Entry<String, MediaType> e = mediaType.get();
                return new MediaTypeInfo(e.getValue(), e.getKey());
            }
            return null;
        }
    }

    public static Map<String, Integer> getAllUsedSchemaRefs(OpenAPI openAPI, JXPathContext jxPathContext) {
        Map<String, Integer> types = new HashMap<>();
        visitOpenAPI(openAPI, jxPathContext, (Schema<?> s) -> {
            String ref = s.get$ref();
            if (ref != null) {
                Schema<?> x = (Schema<?>) OpenLOpenAPIUtils.resolveByRef(jxPathContext, ref);
                if (!OpenAPITypeUtils.isComplexSchema(jxPathContext, x)) {
                    return;
                }
                types.merge(ref, 1, Integer::sum);
            }
        });
        return types;
    }

    public static Map<String, Map<String, Integer>> collectPathsWithParams(OpenAPI openAPI,
            JXPathContext jxPathContext) {
        Map<String, Map<String, Integer>> resultMap = new HashMap<>();
        Set<String> visitedSchemas = new HashSet<>();
        Map<String, PathItem> paths = openAPI.getPaths();
        if (CollectionUtils.isNotEmpty(paths)) {
            for (Map.Entry<String, PathItem> pathWithItem : paths.entrySet()) {
                visitPathItemRequests(pathWithItem.getValue(), jxPathContext, (Schema<?> s) -> {
                    String ref = s.get$ref();
                    if (ref != null) {
                        Schema<?> x = (Schema<?>) OpenLOpenAPIUtils.resolveByRef(jxPathContext, ref);
                        if (!OpenAPITypeUtils.isComplexSchema(jxPathContext, x)) {
                            return;
                        }
                        String path = pathWithItem.getKey();
                        Map<String, Integer> requestRefs = resultMap.get(path);
                        if (requestRefs == null) {
                            requestRefs = new HashMap<>();
                            requestRefs.put(ref, 1);
                        } else {
                            requestRefs.merge(ref, 1, Integer::sum);
                        }
                        resultMap.put(path, requestRefs);
                    }
                }, visitedSchemas);
            }
        }
        return resultMap;
    }

    public static Map<String, Set<String>> getAllUsedRefResponses(OpenAPI openAPI, JXPathContext jxPathContext) {
        Map<String, PathItem> paths = openAPI.getPaths();
        Map<String, Set<String>> allSchemaRefResponses = new HashMap<>();
        if (paths != null) {
            for (Map.Entry<String, PathItem> p : paths.entrySet()) {
                PathItem path = p.getValue();
                Map<PathItem.HttpMethod, Operation> operationsMap = path.readOperationsMap();
                if (CollectionUtils.isNotEmpty(operationsMap)) {
                    Pair<Operation, PathItem.HttpMethod> satisfyingOperationWithMethod = OpenLOpenAPIUtils
                        .getOperation(path);
                    if (satisfyingOperationWithMethod != null) {
                        ApiResponses responses = satisfyingOperationWithMethod.getKey().getResponses();
                        if (responses != null) {
                            ApiResponse response = OpenLOpenAPIUtils.getResponse(jxPathContext, responses);
                            if (response != null && CollectionUtils.isNotEmpty(response.getContent())) {
                                MediaTypeInfo mediaType = OpenLOpenAPIUtils.getMediaType(response.getContent());
                                if (mediaType != null) {
                                    Schema<?> mediaTypeSchema = mediaType.getContent().getSchema();
                                    if (mediaTypeSchema != null) {
                                        Set<String> refs = OpenLOpenAPIUtils
                                            .visitSchema(jxPathContext, mediaTypeSchema, false, false);
                                        allSchemaRefResponses.put(p.getKey(), refs);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return allSchemaRefResponses;
    }

    public static Set<String> visitSchema(JXPathContext jxPathContext,
            Schema<?> schema,
            boolean visitInterfaces,
            boolean visitProperties) {
        Set<String> result = new HashSet<>();
        Set<String> visitedSchema = new HashSet<>();
        visitSchema(jxPathContext, schema, null, visitedSchema, (Schema<?> x) -> {
            String ref = x.get$ref();
            if (ref != null) {
                Schema<?> s = (Schema<?>) OpenLOpenAPIUtils.resolveByRef(jxPathContext, ref);
                if (!OpenAPITypeUtils.isComplexSchema(jxPathContext, s)) {
                    return;
                }
                result.add(ref);
            }
        }, visitInterfaces, visitProperties);
        return result;
    }

    private static void visitOpenAPI(OpenAPI openAPI, JXPathContext jxPathContext, Consumer<Schema<?>> visitor) {
        Map<String, PathItem> paths = openAPI.getPaths();
        Set<String> visitedSchemas = new HashSet<>();

        if (paths != null) {
            for (PathItem path : paths.values()) {
                visitPathItem(path, jxPathContext, visitor, visitedSchemas);
            }
        }
    }

    private static void visitPathItem(PathItem pathItem,
            JXPathContext jxPathContext,
            Consumer<Schema<?>> visitor,
            Set<String> visitedSchemas) {
        List<Operation> allOperations = pathItem.readOperations();
        if (allOperations != null) {
            for (Operation operation : allOperations) {
                // Params:
                visitParameters(jxPathContext, operation.getParameters(), visitor, visitedSchemas, true, true);

                // RequestBody:
                RequestBody requestBody = resolve(jxPathContext, operation.getRequestBody(), RequestBody::get$ref);
                if (requestBody != null) {
                    visitContent(jxPathContext, requestBody.getContent(), visitor, visitedSchemas, true);
                }

                // Responses:
                if (operation.getResponses() != null) {
                    for (ApiResponse r : operation.getResponses().values()) {
                        ApiResponse apiResponse = resolve(jxPathContext, r, ApiResponse::get$ref);
                        if (apiResponse != null) {
                            visitContent(jxPathContext, apiResponse.getContent(), visitor, visitedSchemas, true);
                            if (apiResponse.getHeaders() != null) {
                                for (Map.Entry<String, Header> e : apiResponse.getHeaders().entrySet()) {
                                    Header header = resolve(jxPathContext, e.getValue(), Header::get$ref);
                                    if (header.getSchema() != null) {
                                        visitSchema(jxPathContext,
                                            header.getSchema(),
                                            e.getKey(),
                                            visitedSchemas,
                                            visitor,
                                            true,
                                            true);
                                    }
                                    visitContent(jxPathContext, header.getContent(), visitor, visitedSchemas, true);
                                }
                            }
                        }
                    }
                }

                // Callbacks: openl doesn't support it
                if (operation.getCallbacks() != null) {
                    for (Callback c : operation.getCallbacks().values()) {
                        Callback callback = resolve(jxPathContext, c, Callback::get$ref);
                        if (callback != null) {
                            for (PathItem p : callback.values()) {
                                visitPathItem(p, jxPathContext, visitor, visitedSchemas);
                            }
                        }
                    }
                }
            }
        }
        // Params:
        visitParameters(jxPathContext, pathItem.getParameters(), visitor, visitedSchemas, true, true);
    }

    private static void visitPathItemRequests(PathItem pathItem,
            JXPathContext jxPathContext,
            Consumer<Schema<?>> visitor,
            Set<String> visitedSchemas) {
        List<Operation> allOperations = pathItem.readOperations();
        if (allOperations != null) {
            for (Operation operation : allOperations) {
                // parameters there too
                visitParameters(jxPathContext, operation.getParameters(), visitor, visitedSchemas, false, true);
                // RequestBody:
                RequestBody requestBody = resolve(jxPathContext, operation.getRequestBody(), RequestBody::get$ref);
                if (requestBody != null) {
                    visitContent(jxPathContext, requestBody.getContent(), visitor, visitedSchemas, false);
                }
            }
        }
        visitParameters(jxPathContext, pathItem.getParameters(), visitor, visitedSchemas, false, true);
    }

    private static void visitParameters(JXPathContext jxPathContext,
            List<Parameter> parameters,
            Consumer<Schema<?>> visitor,
            Set<String> visitedSchemas,
            boolean visitInterfaces,
            boolean visitProperties) {
        if (parameters != null) {
            for (Parameter p : parameters) {
                Parameter parameter = resolve(jxPathContext, p, Parameter::get$ref);
                if (parameter != null) {
                    if (parameter.getSchema() != null) {
                        visitSchema(jxPathContext,
                            parameter.getSchema(),
                            null,
                            visitedSchemas,
                            visitor,
                            visitInterfaces,
                            visitProperties);
                    }
                    visitContent(jxPathContext, parameter.getContent(), visitor, visitedSchemas, visitInterfaces);
                } else {
                    LOGGER.warn("An unreferenced parameter(s) found.");
                }
            }
        }
    }

    private static void visitContent(JXPathContext jxPathContext,
            Content content,
            Consumer<Schema<?>> visitor,
            Set<String> visitedSchemas,
            boolean visitInterfaces) {
        if (content != null) {
            for (Map.Entry<String, MediaType> e : content.entrySet()) {
                Schema<?> mediaTypeSchema = e.getValue().getSchema();
                if (mediaTypeSchema != null) {
                    visitSchema(jxPathContext,
                        mediaTypeSchema,
                        e.getKey(),
                        visitedSchemas,
                        visitor,
                        visitInterfaces,
                        true);
                }
            }
        }
    }

    private static void visitSchema(JXPathContext jxPathContext,
            Schema<?> schema,
            String mimeType,
            Set<String> visitedSchemas,
            Consumer<Schema<?>> visitor,
            boolean visitInterfaces,
            boolean visitProperties) {
        visitor.accept(schema);
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            if (!visitedSchemas.contains(ref)) {
                visitedSchemas.add(ref);
                Schema<?> referencedSchema = resolve(jxPathContext, schema, Schema::get$ref);
                if (referencedSchema != null) {
                    visitSchema(jxPathContext,
                        referencedSchema,
                        mimeType,
                        visitedSchemas,
                        visitor,
                        visitInterfaces,
                        visitProperties);
                }
            }
        }
        if (schema instanceof ComposedSchema && visitInterfaces) {
            List<Schema> oneOf = ((ComposedSchema) schema).getOneOf();
            if (oneOf != null) {
                for (Schema<?> s : oneOf) {
                    visitSchema(jxPathContext, s, mimeType, visitedSchemas, visitor, visitInterfaces, visitProperties);
                }
            }
            List<Schema> allOf = ((ComposedSchema) schema).getAllOf();
            if (allOf != null) {
                for (Schema<?> s : allOf) {
                    visitSchema(jxPathContext, s, mimeType, visitedSchemas, visitor, visitInterfaces, visitProperties);
                }
            }

            List<Schema> anyOf = ((ComposedSchema) schema).getAnyOf();
            if (anyOf != null) {
                for (Schema<?> s : anyOf) {
                    visitSchema(jxPathContext, s, mimeType, visitedSchemas, visitor, visitInterfaces, visitProperties);
                }
            }
        } else if (schema instanceof ArraySchema) {
            Schema<?> itemsSchema = ((ArraySchema) schema).getItems();
            if (itemsSchema != null) {
                visitSchema(jxPathContext,
                    itemsSchema,
                    mimeType,
                    visitedSchemas,
                    visitor,
                    visitInterfaces,
                    visitProperties);
            }
        } else if (isMapSchema(schema)) {
            Object additionalProperties = schema.getAdditionalProperties();
            if (additionalProperties instanceof Schema) {
                visitSchema(jxPathContext,
                    (Schema) additionalProperties,
                    mimeType,
                    visitedSchemas,
                    visitor,
                    visitInterfaces,
                    visitProperties);
            }
        }
        if (schema.getNot() != null) {
            visitSchema(jxPathContext,
                schema.getNot(),
                mimeType,
                visitedSchemas,
                visitor,
                visitInterfaces,
                visitProperties);
        }
        if (visitProperties) {
            Map<String, Schema> properties = schema.getProperties();
            if (properties != null) {
                for (Schema<?> property : properties.values()) {
                    visitSchema(jxPathContext,
                        property,
                        mimeType,
                        visitedSchemas,
                        visitor,
                        visitInterfaces,
                        visitProperties);
                }
            }
        }
    }

    private static boolean isMapSchema(Schema<?> schema) {
        if (schema instanceof MapSchema) {
            return true;
        }

        if (schema == null) {
            return false;
        }

        if (schema.getAdditionalProperties() instanceof Schema) {
            return true;
        }

        return schema.getAdditionalProperties() instanceof Boolean && (Boolean) schema.getAdditionalProperties();
    }

    public static Map<String, Schema> getSchemas(OpenAPI openAPI) {
        if (openAPI != null && openAPI.getComponents() != null && CollectionUtils
            .isNotEmpty(openAPI.getComponents().getSchemas())) {
            return openAPI.getComponents().getSchemas();
        }
        return Collections.emptyMap();
    }

    public static Map<String, Schema> getAllFields(OpenAPI openAPI, ComposedSchema cs) {
        Map<String, Schema> propMap = new HashMap<>();
        List<Schema> interfaces = getInterfaces(cs);
        if (CollectionUtils.isNotEmpty(interfaces)) {
            for (Schema<?> sc : interfaces) {
                if (StringUtils.isEmpty(sc.get$ref()) && CollectionUtils.isNotEmpty(sc.getProperties())) {
                    propMap.putAll(sc.getProperties());
                } else if (!StringUtils.isEmpty(sc.get$ref())) {
                    Schema<?> s = getSchemas(openAPI).get(OpenAPITypeUtils.getSimpleName(sc.get$ref()));
                    if (s != null && CollectionUtils.isNotEmpty(s.getProperties())) {
                        propMap.putAll(s.getProperties());
                    }
                }
            }
        }
        return propMap;
    }

    public static Map<String, Set<String>> getRefsInProperties(OpenAPI openAPI, JXPathContext jxPathContext) {
        Map<String, Set<String>> refs = new HashMap<>();
        Map<String, Schema> schemas = getSchemas(openAPI);
        if (CollectionUtils.isNotEmpty(schemas)) {
            for (Map.Entry<String, Schema> schema : schemas.entrySet()) {
                Set<String> schemaRefs = new HashSet<>(visitSchema(jxPathContext, schema.getValue(), false, true));
                if (CollectionUtils.isNotEmpty(schemaRefs)) {
                    refs.put(SCHEMAS_LINK + schema.getKey(), schemaRefs);
                }
            }
        }
        return refs;
    }

    public static List<Schema> getInterfaces(ComposedSchema composed) {
        List<Schema> result;
        if (composed.getAllOf() != null && !composed.getAllOf().isEmpty()) {
            result = composed.getAllOf();
        } else if (composed.getAnyOf() != null && !composed.getAnyOf().isEmpty()) {
            result = composed.getAnyOf();
        } else if (composed.getOneOf() != null && !composed.getOneOf().isEmpty()) {
            result = composed.getOneOf();
        } else {
            result = Collections.emptyList();
        }
        return result;
    }

    public static List<InputParameter> extractParameters(JXPathContext jxPathContext,
            OpenAPI openAPI,
            Set<String> refsToExpand,
            PathItem pathItem) {
        return visitPathItemForParametersOfRequest(jxPathContext, openAPI, pathItem, refsToExpand);
    }

    private static List<InputParameter> visitPathItemForParametersOfRequest(JXPathContext jxPathContext,
            OpenAPI openAPI,
            PathItem pathItem,
            Set<String> refsToExpand) {
        List<InputParameter> parameterModels = new ArrayList<>();
        List<Parameter> pathParameters = pathItem.getParameters();
        boolean pathParametersArePresented = CollectionUtils.isNotEmpty(pathParameters);
        if (pathParametersArePresented) {
            parameterModels.addAll(collectInputParams(openAPI, jxPathContext, pathParameters, refsToExpand, true));
        }
        Pair<Operation, PathItem.HttpMethod> satisfyingOperationWithMethod = OpenLOpenAPIUtils.getOperation(pathItem);
        if (satisfyingOperationWithMethod != null) {
            Operation satisfyingOperation = satisfyingOperationWithMethod.getLeft();
            PathItem.HttpMethod method = satisfyingOperationWithMethod.getRight();
            List<Parameter> parameters = satisfyingOperation.getParameters();
            boolean allowPrimitiveTypes = method.equals(PathItem.HttpMethod.GET);
            boolean operationsParametersArePresented = CollectionUtils.isNotEmpty(parameters);
            if (operationsParametersArePresented) {
                parameterModels
                    .addAll(collectInputParams(openAPI, jxPathContext, parameters, refsToExpand, allowPrimitiveTypes));
            }
            RequestBody requestBody = resolve(jxPathContext,
                satisfyingOperation.getRequestBody(),
                RequestBody::get$ref);
            if (requestBody != null && CollectionUtils.isNotEmpty(requestBody.getContent())) {
                MediaTypeInfo mediaType = OpenLOpenAPIUtils.getMediaType(requestBody.getContent());
                if (mediaType != null) {
                    MediaType content = mediaType.getContent();
                    Schema<?> resSchema = resolve(jxPathContext, content.getSchema(), Schema::get$ref);
                    parameterModels.addAll(collectInputParams(jxPathContext,
                        openAPI,
                        refsToExpand,
                        mediaType,
                        resSchema,
                        pathParametersArePresented || operationsParametersArePresented));
                }
            }
        }
        return parameterModels;
    }

    public static String normalizeName(String originalName) {
        StringBuilder resultName = new StringBuilder();
        for (int i = 0; i < originalName.length(); i++) {
            char curChar = originalName.charAt(i);
            if (resultName.length() == 0) {
                if (Character.isJavaIdentifierStart(curChar)) {
                    resultName.append(curChar);
                }
            } else {
                if (Character.isJavaIdentifierPart(curChar)) {
                    resultName.append(curChar);
                }
            }
        }
        return resultName.toString();
    }

    private static List<InputParameter> collectInputParams(OpenAPI openAPI,
            JXPathContext jxPathContext,
            Collection<Parameter> params,
            Set<String> refsToExpand,
            boolean allowPrimitiveTypes) {
        List<InputParameter> result = new ArrayList<>();
        for (Parameter param : params) {
            Parameter p = resolve(jxPathContext, param, Parameter::get$ref);
            if (p != null) {
                Schema<?> paramSchema = p.getSchema();
                if (paramSchema != null) {
                    Schema<?> resSchema = resolve(jxPathContext, paramSchema, Schema::get$ref);
                    String ref = paramSchema.get$ref();
                    if (ref != null && refsToExpand.contains(ref)) {
                        result.addAll(collectParameters(openAPI, jxPathContext, refsToExpand, resSchema, ref));
                    } else {
                        if (paramSchema instanceof ArraySchema) {
                            refsToExpand.removeIf(x -> x.equals(((ArraySchema) paramSchema).getItems().get$ref()));
                        }
                        ParameterModel parameterModel = new ParameterModel(
                            OpenAPITypeUtils.extractType(jxPathContext, paramSchema, allowPrimitiveTypes),
                            normalizeName(p.getName()),
                            p.getName());
                        Optional.ofNullable(p.getIn())
                            .map(String::toUpperCase)
                            .map(InputParameter.In::valueOf)
                            .ifPresent(parameterModel::setIn);
                        result.add(parameterModel);
                    }
                }
            }
        }
        return result;
    }

    private static List<InputParameter> collectParameters(OpenAPI openAPI,
            JXPathContext jxPathContext,
            Set<String> refsToExpand,
            Schema<?> paramSchema,
            String ref) {
        List<InputParameter> result = new ArrayList<>();
        Map<String, Schema> properties;
        if (paramSchema instanceof ComposedSchema) {
            ComposedSchema cs = (ComposedSchema) paramSchema;
            properties = getAllFields(openAPI, cs);
        } else {
            properties = paramSchema.getProperties();
        }
        if (CollectionUtils.isNotEmpty(properties)) {
            int propertiesCount = properties.size();
            if (propertiesCount == MIN_PARAMETERS_COUNT) {
                refsToExpand.remove(ref);
                String name = OpenAPITypeUtils.getSimpleName(ref);
                TypeInfo typeInfo;
                if (DEFAULT_RUNTIME_CONTEXT.equals(name)) {
                    typeInfo = RUNTIME_CONTEXT_TYPE;
                } else {
                    typeInfo = new TypeInfo(name, name, TypeInfo.Type.DATATYPE);
                }
                ParameterModel parameterModel = new ParameterModel(typeInfo,
                    StringUtils.uncapitalize(normalizeName(name)),
                    name);
                result = Collections.singletonList(parameterModel);
            } else {
                result = properties.entrySet()
                    .stream()
                    .map(p -> OpenLOpenAPIUtils.extractParameter(p, jxPathContext))
                    .collect(Collectors.toList());
            }
        }
        return result;
    }

    private static List<InputParameter> collectInputParams(JXPathContext jxPathContext,
            OpenAPI openAPI,
            Set<String> refsToExpand,
            MediaTypeInfo mediaType,
            Schema<?> resSchema,
            boolean parametersArePresented) {
        List<InputParameter> result = new ArrayList<>();
        if (resSchema != null) {
            // search for refsToExpandInside
            // go through the schema and all the parameters
            Set<String> allSchemasUsedInRequest = visitSchema(jxPathContext, resSchema, false, true);
            boolean requestBodyHasExpandableParam = allSchemasUsedInRequest.stream().anyMatch(refsToExpand::contains);
            if (requestBodyHasExpandableParam) {
                for (String internalModel : allSchemasUsedInRequest) {
                    refsToExpand.remove(internalModel);
                }
            }
            String ref = mediaType.getContent().getSchema().get$ref();
            boolean refIsPresented = ref != null;
            if (parametersArePresented && refIsPresented) {
                refsToExpand.remove(ref);
            }
            // only root schema is expandable
            if (refIsPresented && refsToExpand.contains(ref)) {
                result = collectParameters(openAPI, jxPathContext, refsToExpand, resSchema, ref);
            } else {
                // non expandable
                TypeInfo typeInfo = OpenAPITypeUtils
                    .extractType(jxPathContext, mediaType.getContent().getSchema(), false);
                String type = typeInfo.getSimpleName();
                if (StringUtils.isBlank(type)) {
                    result = Collections.emptyList();
                } else {
                    String parameter = type;
                    if (type.endsWith("[]")) {
                        parameter = OpenAPITypeUtils.removeArrayBrackets(type);
                    }
                    if (OpenAPITypeUtils.isPrimitiveType(type)) {
                        parameter += "Param";
                    }
                    result = new ArrayList<>(Collections
                        .singletonList((new ParameterModel(typeInfo, StringUtils.uncapitalize(parameter), parameter))));
                }
            }
        }
        return result;
    }

    public static ParameterModel extractParameter(Map.Entry<String, Schema> property, JXPathContext jxPathContext) {
        String propertyName = property.getKey();
        Schema<?> valueSchema = property.getValue();
        TypeInfo typeModel = OpenAPITypeUtils.extractType(jxPathContext, valueSchema, false);
        return new ParameterModel(typeModel, normalizeName(propertyName), propertyName);
    }

    public static boolean checkVariations(OpenAPI openAPI, Set<String> variationsSchemasName) {
        return getSchemas(openAPI).keySet().containsAll(variationsSchemasName);
    }
}
